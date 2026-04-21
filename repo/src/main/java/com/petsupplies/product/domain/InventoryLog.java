package com.petsupplies.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_logs")
public class InventoryLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "merchant_id", nullable = false, length = 64)
  private String merchantId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sku_id", nullable = false)
  private Sku sku;

  @Column(name = "event_type", nullable = false, length = 32)
  private String eventType;

  @Column(name = "quantity_before", nullable = false)
  private int quantityBefore;

  @Column(name = "quantity_after", nullable = false)
  private int quantityAfter;

  @Column(name = "reference_kind", length = 32)
  private String referenceKind;

  @Column(name = "actor_username", length = 80)
  private String actorUsername;

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

  public Sku getSku() {
    return sku;
  }

  public void setSku(Sku sku) {
    this.sku = sku;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public int getQuantityBefore() {
    return quantityBefore;
  }

  public void setQuantityBefore(int quantityBefore) {
    this.quantityBefore = quantityBefore;
  }

  public int getQuantityAfter() {
    return quantityAfter;
  }

  public void setQuantityAfter(int quantityAfter) {
    this.quantityAfter = quantityAfter;
  }

  public String getReferenceKind() {
    return referenceKind;
  }

  public void setReferenceKind(String referenceKind) {
    this.referenceKind = referenceKind;
  }

  public String getActorUsername() {
    return actorUsername;
  }

  public void setActorUsername(String actorUsername) {
    this.actorUsername = actorUsername;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
