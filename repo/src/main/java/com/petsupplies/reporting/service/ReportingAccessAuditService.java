package com.petsupplies.reporting.service;

import com.petsupplies.auditing.service.AuditService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Append-only audit records for reporting <strong>read</strong> paths (admin / reviewer / merchant custom-report views).
 */
@Service
public class ReportingAccessAuditService {
  private final AuditService auditService;

  public ReportingAccessAuditService(AuditService auditService) {
    this.auditService = auditService;
  }

  public void record(
      String eventType,
      String actorUsername,
      String remoteAddr,
      Map<String, Object> metadata
  ) {
    Map<String, Object> payload = new LinkedHashMap<>(metadata);
    payload.put("surface", "REPORT_READ");
    auditService.record(eventType, payload, actorUsername, remoteAddr);
  }
}
