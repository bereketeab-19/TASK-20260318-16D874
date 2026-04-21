package com.petsupplies.auditing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsupplies.auditing.domain.AuditLog;
import com.petsupplies.auditing.repo.AuditLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper, Clock clock) {
    this.auditLogRepository = auditLogRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(String action, Map<String, Object> payload, String actorUsername, String ip) {
    String jsonPayload = safeJson(payload);
    AuditLog log = new AuditLog(action, jsonPayload, Instant.now(clock), actorUsername, ip);
    auditLogRepository.save(log);
  }

  private String safeJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      return "{\"error\":\"payload_serialization_failed\"}";
    }
  }
}

