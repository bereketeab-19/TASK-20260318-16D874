package com.petsupplies.cooking.domain;

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
@Table(name = "cooking_processes")
public class CookingProcess {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "merchant_id", nullable = false, length = 64)
  private String merchantId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private CookingStatus status;

  @Column(name = "current_step_index", nullable = false)
  private int currentStepIndex;

  @Column(name = "last_checkpoint_at", nullable = false)
  private Instant lastCheckpointAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Long getId() {
    return id;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public CookingStatus getStatus() {
    return status;
  }

  public void setStatus(CookingStatus status) {
    this.status = status;
  }

  public int getCurrentStepIndex() {
    return currentStepIndex;
  }

  public void setCurrentStepIndex(int currentStepIndex) {
    this.currentStepIndex = currentStepIndex;
  }

  public Instant getLastCheckpointAt() {
    return lastCheckpointAt;
  }

  public void setLastCheckpointAt(Instant lastCheckpointAt) {
    this.lastCheckpointAt = lastCheckpointAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}

