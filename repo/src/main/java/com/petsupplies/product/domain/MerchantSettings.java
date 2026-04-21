package com.petsupplies.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "merchant_settings")
public class MerchantSettings {
  @Id
  @Column(name = "merchant_id", nullable = false, length = 64)
  private String merchantId;

  @Column(name = "low_stock_threshold", nullable = false)
  private int lowStockThreshold = 10;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public int getLowStockThreshold() {
    return lowStockThreshold;
  }

  public void setLowStockThreshold(int lowStockThreshold) {
    this.lowStockThreshold = lowStockThreshold;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
