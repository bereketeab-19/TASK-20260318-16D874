package com.petsupplies.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "custom_report_definitions")
public class CustomReportDefinition {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "merchant_id", nullable = false, length = 64)
  private String merchantId;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "definition_json", nullable = false, columnDefinition = "TEXT")
  private String definitionJson;

  @Column(name = "schedule_cron", length = 120)
  private String scheduleCron;

  @Column(name = "schedule_timezone", length = 64)
  private String scheduleTimezone;

  @Column(name = "owner_user_id", nullable = false)
  private Long ownerUserId;

  @Column(name = "active", nullable = false)
  private boolean active = true;

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDefinitionJson() {
    return definitionJson;
  }

  public void setDefinitionJson(String definitionJson) {
    this.definitionJson = definitionJson;
  }

  public String getScheduleCron() {
    return scheduleCron;
  }

  public void setScheduleCron(String scheduleCron) {
    this.scheduleCron = scheduleCron;
  }

  public String getScheduleTimezone() {
    return scheduleTimezone;
  }

  public void setScheduleTimezone(String scheduleTimezone) {
    this.scheduleTimezone = scheduleTimezone;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
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
