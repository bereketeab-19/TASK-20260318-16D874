package com.petsupplies.auditing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsupplies.auditing.domain.CriticalOperationType;
import com.petsupplies.auditing.domain.PendingApproval;
import com.petsupplies.auditing.domain.PendingApprovalStatus;
import com.petsupplies.auditing.repo.PendingApprovalRepository;
import com.petsupplies.product.domain.Category;
import com.petsupplies.product.repo.CategoryRepository;
import com.petsupplies.product.repo.ProductRepository;
import com.petsupplies.settings.domain.SystemConfig;
import com.petsupplies.settings.repo.SystemConfigRepository;
import com.petsupplies.user.domain.Role;
import com.petsupplies.user.repo.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Dual-approval execution for {@link CriticalOperationType}: {@code PERMISSION_CHANGE},
 * {@code SYSTEM_CONFIG_UPDATE}, {@code DATA_WIPE_OR_RESTORE}, and {@code ACTIVE_CATEGORY_DELETION}.
 */
@Service
public class PendingApprovalService {
  private static final Logger log = LoggerFactory.getLogger(PendingApprovalService.class);
  private static final Pattern CONFIG_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_.\\-]+$");

  private final PendingApprovalRepository pendingApprovalRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final AuditService auditService;
  private final Clock clock;
  private final SystemConfigRepository systemConfigRepository;
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;

  public PendingApprovalService(
      PendingApprovalRepository pendingApprovalRepository,
      UserRepository userRepository,
      ObjectMapper objectMapper,
      AuditService auditService,
      Clock clock,
      SystemConfigRepository systemConfigRepository,
      CategoryRepository categoryRepository,
      ProductRepository productRepository
  ) {
    this.pendingApprovalRepository = pendingApprovalRepository;
    this.userRepository = userRepository;
    this.objectMapper = objectMapper;
    this.auditService = auditService;
    this.clock = clock;
    this.systemConfigRepository = systemConfigRepository;
    this.categoryRepository = categoryRepository;
    this.productRepository = productRepository;
  }

  @Transactional
  public PendingApproval request(
      CriticalOperationType type,
      JsonNode payload,
      Long requesterUserId,
      String requesterUsername
  ) {
    PendingApproval p = new PendingApproval();
    p.setOperationType(type);
    p.setPayloadJson(payload.toString());
    p.setStatus(PendingApprovalStatus.PENDING);
    p.setRequesterUserId(requesterUserId);
    p.setRequesterUsername(requesterUsername);
    p.setCreatedAt(Instant.now(clock));
    PendingApproval saved = pendingApprovalRepository.save(p);

    auditService.record(
        "APPROVAL_REQUESTED",
        Map.of("approvalId", saved.getId(), "operationType", type.name(), "requesterUserId", requesterUserId),
        requesterUsername,
        null
    );

    return saved;
  }

  @Transactional
  public PendingApproval execute(Long approvalId, Long approverUserId, String approverUsername) {
    PendingApproval p = pendingApprovalRepository.findByIdAndStatus(approvalId, PendingApprovalStatus.PENDING)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

    if (p.getRequesterUserId().equals(approverUserId)) {
      auditService.record(
          "APPROVAL_SELF_APPROVAL_BLOCKED",
          Map.of("approvalId", approvalId, "requesterUserId", p.getRequesterUserId(), "approverUserId", approverUserId),
          approverUsername,
          null
      );
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Self-approval is not allowed");
    }

    // Execute operation
    switch (p.getOperationType()) {
      case PERMISSION_CHANGE -> executePermissionChange(p.getPayloadJson(), approverUsername);
      case SYSTEM_CONFIG_UPDATE -> executeSystemConfigUpdate(p.getPayloadJson());
      case DATA_WIPE_OR_RESTORE -> executeDataWipeOrRestore(p.getPayloadJson());
      case ACTIVE_CATEGORY_DELETION -> executeActiveCategoryDeletion(p.getPayloadJson());
    }

    Instant now = Instant.now(clock);
    p.setStatus(PendingApprovalStatus.APPROVED);
    p.setApproverUserId(approverUserId);
    p.setApproverUsername(approverUsername);
    p.setDecidedAt(now);
    p.setExecutedAt(now);

    auditService.record(
        "APPROVAL_EXECUTED",
        Map.of(
            "approvalId", approvalId,
            "operationType", p.getOperationType().name(),
            "requesterUserId", p.getRequesterUserId(),
            "approverUserId", approverUserId
        ),
        approverUsername,
        null
    );

    return p;
  }

  @Transactional
  public PendingApproval reject(Long approvalId, Long approverUserId, String approverUsername) {
    PendingApproval p = pendingApprovalRepository.findByIdAndStatus(approvalId, PendingApprovalStatus.PENDING)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

    if (p.getRequesterUserId().equals(approverUserId)) {
      auditService.record(
          "APPROVAL_SELF_REJECT_BLOCKED",
          Map.of("approvalId", approvalId, "requesterUserId", p.getRequesterUserId(), "approverUserId", approverUserId),
          approverUsername,
          null
      );
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Self-reject is not allowed");
    }

    Instant now = Instant.now(clock);
    p.setStatus(PendingApprovalStatus.REJECTED);
    p.setApproverUserId(approverUserId);
    p.setApproverUsername(approverUsername);
    p.setDecidedAt(now);

    auditService.record(
        "APPROVAL_REJECTED",
        Map.of("approvalId", approvalId, "requesterUserId", p.getRequesterUserId(), "approverUserId", approverUserId),
        approverUsername,
        null
    );

    return p;
  }

  private void executePermissionChange(String payloadJson, String approverUsername) {
    JsonNode root = readJson(payloadJson);
    String username = root.path("targetUsername").asText(null);
    String roleStr = root.path("newRole").asText(null);
    if (username == null || roleStr == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload");
    }
    Role newRole;
    try {
      newRole = Role.valueOf(roleStr);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
    }

    var user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

    String merchantFromPayload = root.path("merchantId").asText(null);
    if (merchantFromPayload != null) {
      merchantFromPayload = merchantFromPayload.isBlank() ? null : merchantFromPayload.trim();
    }

    if (newRole == Role.MERCHANT) {
      if (merchantFromPayload == null || merchantFromPayload.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantId is required when newRole is MERCHANT");
      }
      user.setMerchantId(merchantFromPayload);
    } else {
      user.setMerchantId(null);
    }
    user.setRole(newRole);
    userRepository.save(user);
    log.info(
        "Permission change applied target={} newRole={} approver={}",
        username,
        newRole.name(),
        approverUsername
    );
  }

  private void executeSystemConfigUpdate(String payloadJson) {
    JsonNode root = readJson(payloadJson);
    String key = root.path("configKey").asText(null);
    String value = root.path("configValue").asText(null);
    if (key == null || value == null || !CONFIG_KEY_PATTERN.matcher(key).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid configKey/configValue");
    }
    upsertSystemConfig(key, value);
  }

  private void executeDataWipeOrRestore(String payloadJson) {
    JsonNode root = readJson(payloadJson);
    String action = root.path("action").asText(null);
    if (!"REGISTER_BACKUP_VERIFICATION".equals(action)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Only action=REGISTER_BACKUP_VERIFICATION is supported in offline mode (no destructive wipe)"
      );
    }
    String label = root.path("backupLabel").asText(null);
    if (label == null || label.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "backupLabel required");
    }
    Instant now = Instant.now(clock);
    upsertSystemConfig("backup.last_verified_label", label.trim(), now);
    upsertSystemConfig("backup.last_verified_at", now.toString(), now);
  }

  private void executeActiveCategoryDeletion(String payloadJson) {
    JsonNode root = readJson(payloadJson);
    String merchantId = root.path("merchantId").asText(null);
    Long categoryId = root.path("categoryId").isIntegralNumber() ? root.path("categoryId").longValue() : null;
    if (merchantId == null || merchantId.isBlank() || categoryId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantId and categoryId required");
    }
    Category cat = categoryRepository.findByIdAndMerchantId(categoryId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    if (productRepository.countByCategory_Id(cat.getId()) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category still has products");
    }
    if (categoryRepository.countByParent_Id(cat.getId()) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category has child categories");
    }
    categoryRepository.delete(cat);
  }

  private JsonNode readJson(String payloadJson) {
    try {
      return objectMapper.readTree(payloadJson);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload");
    }
  }

  private void upsertSystemConfig(String key, String value) {
    upsertSystemConfig(key, value, Instant.now(clock));
  }

  private void upsertSystemConfig(String key, String value, Instant updatedAt) {
    SystemConfig row = systemConfigRepository.findById(key).orElse(new SystemConfig());
    row.setConfigKey(key);
    row.setConfigValue(value);
    row.setUpdatedAt(updatedAt);
    systemConfigRepository.save(row);
  }
}

