package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petsupplies.auditing.repo.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ReportingReadAuditIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired AuditLogRepository auditLogRepository;

  @Test
  void admin_report_definitions_read_writes_audit_row() throws Exception {
    mockMvc.perform(get("/api/admin/reports/definitions").with(httpBasic("admin", "admin123!")))
        .andExpect(status().isOk());

    assertThat(auditLogRepository.findFirstByActionOrderByIdDesc("ADMIN_REPORT_DEFINITIONS_READ"))
        .isPresent();
  }
}
