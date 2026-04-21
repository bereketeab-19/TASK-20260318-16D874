package com.petsupplies.achievement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "achievements")
public class Achievement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "merchant_id", nullable = false, length = 64)
  private String merchantId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "period", nullable = false, length = 120)
  private String period;

  @Column(name = "responsible_person", nullable = false, length = 120)
  private String responsiblePerson;

  @Column(name = "conclusion", nullable = false, columnDefinition = "TEXT")
  private String conclusion;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public String getResponsiblePerson() {
    return responsiblePerson;
  }

  public void setResponsiblePerson(String responsiblePerson) {
    this.responsiblePerson = responsiblePerson;
  }

  public String getConclusion() {
    return conclusion;
  }

  public void setConclusion(String conclusion) {
    this.conclusion = conclusion;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}

