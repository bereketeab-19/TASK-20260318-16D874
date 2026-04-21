package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsupplies.auditing.repo.AuditLogRepository;
import com.petsupplies.auditing.repo.PendingApprovalRepository;
import com.petsupplies.auditing.domain.PendingApprovalStatus;
import com.petsupplies.product.domain.Category;
import com.petsupplies.product.repo.CategoryRepository;
import com.petsupplies.settings.repo.SystemConfigRepository;
import com.petsupplies.user.domain.Role;
import com.petsupplies.user.repo.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class DualApprovalIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired UserRepository userRepository;
  @Autowired AuditLogRepository auditLogRepository;
  @Autowired PendingApprovalRepository pendingApprovalRepository;
  @Autowired SystemConfigRepository systemConfigRepository;
  @Autowired CategoryRepository categoryRepository;

  @AfterEach
  void restoreBuyerRole() {
    var buyer = userRepository.findByUsername("buyer").orElseThrow();
    buyer.setRole(Role.BUYER);
    buyer.setMerchantId(null);
    buyer.setFailedAttempts(0);
    buyer.setLockedUntil(null);
    userRepository.save(buyer);
  }

  @Test
  void system_config_update_requires_second_admin_and_persists() throws Exception {
    var reqBody = Map.of(
        "operationType", "SYSTEM_CONFIG_UPDATE",
        "payload", Map.of("configKey", "test.feature", "configValue", "enabled")
    );

    var created = mockMvc.perform(post("/api/admin/approvals/request")
            .with(httpBasic("admin", "admin123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(reqBody)))
        .andExpect(status().isOk())
        .andReturn();

    long approvalId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post("/api/admin/approvals/" + approvalId + "/execute")
            .with(httpBasic("admin2", "admin2_123!")))
        .andExpect(status().isOk());

    assertThat(systemConfigRepository.findById("test.feature"))
        .isPresent();
    assertThat(systemConfigRepository.findById("test.feature").orElseThrow().getConfigValue())
        .isEqualTo("enabled");
  }

  @Test
  void data_wipe_or_restore_registers_backup_verification() throws Exception {
    var reqBody = Map.of(
        "operationType", "DATA_WIPE_OR_RESTORE",
        "payload", Map.of("action", "REGISTER_BACKUP_VERIFICATION", "backupLabel", "nightly-2026-04-21")
    );

    var created = mockMvc.perform(post("/api/admin/approvals/request")
            .with(httpBasic("admin", "admin123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(reqBody)))
        .andExpect(status().isOk())
        .andReturn();

    long approvalId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post("/api/admin/approvals/" + approvalId + "/execute")
            .with(httpBasic("admin2", "admin2_123!")))
        .andExpect(status().isOk());

    assertThat(systemConfigRepository.findById("backup.last_verified_label"))
        .isPresent();
    assertThat(systemConfigRepository.findById("backup.last_verified_label").orElseThrow().getConfigValue())
        .isEqualTo("nightly-2026-04-21");
  }

  @Test
  void active_category_deletion_executes_when_category_has_no_products_or_children() throws Exception {
    var cat = new Category();
    cat.setMerchantId("mrc_A");
    cat.setName("DualApprovalDeleteMe");
    cat.setLevel(1);
    categoryRepository.save(cat);
    long categoryId = cat.getId();

    var reqBody = Map.of(
        "operationType", "ACTIVE_CATEGORY_DELETION",
        "payload", Map.of("merchantId", "mrc_A", "categoryId", categoryId)
    );

    var created = mockMvc.perform(post("/api/admin/approvals/request")
            .with(httpBasic("admin", "admin123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(reqBody)))
        .andExpect(status().isOk())
        .andReturn();

    long approvalId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post("/api/admin/approvals/" + approvalId + "/execute")
            .with(httpBasic("admin2", "admin2_123!")))
        .andExpect(status().isOk());

    assertThat(categoryRepository.findById(categoryId)).isEmpty();
  }

  @Test
  void self_approval_is_blocked_and_different_admin_can_execute() throws Exception {
    // Request permission change: promote buyer -> REVIEWER
    var reqBody = Map.of(
        "operationType", "PERMISSION_CHANGE",
        "payload", Map.of("targetUsername", "buyer", "newRole", "REVIEWER")
    );

    var created = mockMvc.perform(post("/api/admin/approvals/request")
            .with(httpBasic("admin", "admin123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(reqBody)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andReturn();

    long approvalId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    long auditBefore = auditLogRepository.count();

    // Same admin cannot execute
    mockMvc.perform(post("/api/admin/approvals/" + approvalId + "/execute")
            .with(httpBasic("admin", "admin123!")))
        .andExpect(status().isBadRequest());

    // Different admin executes
    mockMvc.perform(post("/api/admin/approvals/" + approvalId + "/execute")
            .with(httpBasic("admin2", "admin2_123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));

    var approval = pendingApprovalRepository.findById(approvalId).orElseThrow();
    assertThat(approval.getStatus()).isEqualTo(PendingApprovalStatus.APPROVED);
    assertThat(approval.getRequesterUserId()).isNotNull();
    assertThat(approval.getApproverUserId()).isNotNull();
    assertThat(approval.getRequesterUserId()).isNotEqualTo(approval.getApproverUserId());

    assertThat(userRepository.findByUsername("buyer").orElseThrow().getRole()).isEqualTo(Role.REVIEWER);
    assertThat(auditLogRepository.count()).isGreaterThan(auditBefore);
  }

  @Test
  void reject_does_not_execute_payload_and_persists_approver_ids() throws Exception {
    // Precondition: set buyer to REVIEWER to ensure reject doesn't change it.
    var buyer = userRepository.findByUsername("buyer").orElseThrow();
    buyer.setRole(Role.REVIEWER);
    userRepository.save(buyer);

    var reqBody = Map.of(
        "operationType", "PERMISSION_CHANGE",
        "payload", Map.of("targetUsername", "buyer", "newRole", "ADMIN")
    );

    var created = mockMvc.perform(post("/api/admin/approvals/request")
            .with(httpBasic("admin", "admin123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(reqBody)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andReturn();

    long approvalId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post("/api/admin/approvals/" + approvalId + "/reject")
            .with(httpBasic("admin2", "admin2_123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));

    var approval = pendingApprovalRepository.findById(approvalId).orElseThrow();
    assertThat(approval.getStatus()).isEqualTo(PendingApprovalStatus.REJECTED);
    assertThat(approval.getRequesterUserId()).isNotNull();
    assertThat(approval.getApproverUserId()).isNotNull();
    assertThat(approval.getRequesterUserId()).isNotEqualTo(approval.getApproverUserId());

    // Payload was NOT executed
    assertThat(userRepository.findByUsername("buyer").orElseThrow().getRole()).isEqualTo(Role.REVIEWER);
  }

  @Test
  void permission_change_to_merchant_requires_merchant_id() throws Exception {
    var buyer = userRepository.findByUsername("buyer").orElseThrow();
    buyer.setRole(Role.BUYER);
    buyer.setMerchantId(null);
    userRepository.save(buyer);

    var reqBody = Map.of(
        "operationType", "PERMISSION_CHANGE",
        "payload", Map.of("targetUsername", "buyer", "newRole", "MERCHANT")
    );

    var created = mockMvc.perform(post("/api/admin/approvals/request")
            .with(httpBasic("admin", "admin123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(reqBody)))
        .andExpect(status().isOk())
        .andReturn();

    long approvalId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post("/api/admin/approvals/" + approvalId + "/execute")
            .with(httpBasic("admin2", "admin2_123!")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void permission_change_to_merchant_with_merchant_id_succeeds() throws Exception {
    var buyer = userRepository.findByUsername("buyer").orElseThrow();
    buyer.setRole(Role.BUYER);
    buyer.setMerchantId(null);
    userRepository.save(buyer);

    var reqBody = Map.of(
        "operationType", "PERMISSION_CHANGE",
        "payload", Map.of("targetUsername", "buyer", "newRole", "MERCHANT", "merchantId", "mrc_A")
    );

    var created = mockMvc.perform(post("/api/admin/approvals/request")
            .with(httpBasic("admin", "admin123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(reqBody)))
        .andExpect(status().isOk())
        .andReturn();

    long approvalId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post("/api/admin/approvals/" + approvalId + "/execute")
            .with(httpBasic("admin2", "admin2_123!")))
        .andExpect(status().isOk());

    var b = userRepository.findByUsername("buyer").orElseThrow();
    assertThat(b.getRole()).isEqualTo(Role.MERCHANT);
    assertThat(b.getMerchantId()).isEqualTo("mrc_A");
  }
}

