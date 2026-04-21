package com.petsupplies.auditing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "pending_approvals")
public class PendingApproval {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false, length = 64)
  private CriticalOperationType operationType;

  @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
  private String payloadJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private PendingApprovalStatus status;

  @Column(name = "requester_user_id", nullable = false)
  private Long requesterUserId;

  @Column(name = "requester_username", nullable = false, length = 80)
  private String requesterUsername;

  @Column(name = "approver_user_id")
  private Long approverUserId;

  @Column(name = "approver_username", length = 80)
  private String approverUsername;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "executed_at")
  private Instant executedAt;

  public Long getId() {
    return id;
  }

  public CriticalOperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(CriticalOperationType operationType) {
    this.operationType = operationType;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(String payloadJson) {
    this.payloadJson = payloadJson;
  }

  public PendingApprovalStatus getStatus() {
    return status;
  }

  public void setStatus(PendingApprovalStatus status) {
    this.status = status;
  }

  public Long getRequesterUserId() {
    return requesterUserId;
  }

  public void setRequesterUserId(Long requesterUserId) {
    this.requesterUserId = requesterUserId;
  }

  public String getRequesterUsername() {
    return requesterUsername;
  }

  public void setRequesterUsername(String requesterUsername) {
    this.requesterUsername = requesterUsername;
  }

  public Long getApproverUserId() {
    return approverUserId;
  }

  public void setApproverUserId(Long approverUserId) {
    this.approverUserId = approverUserId;
  }

  public String getApproverUsername() {
    return approverUsername;
  }

  public void setApproverUsername(String approverUsername) {
    this.approverUsername = approverUsername;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(Instant decidedAt) {
    this.decidedAt = decidedAt;
  }

  public Instant getExecutedAt() {
    return executedAt;
  }

  public void setExecutedAt(Instant executedAt) {
    this.executedAt = executedAt;
  }
}

