package com.petsupplies.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_inventory_reports")
public class DailyInventoryReport {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "merchant_id", nullable = false, length = 64)
  private String merchantId;

  @Column(name = "report_date", nullable = false)
  private LocalDate reportDate;

  @Column(name = "total_skus", nullable = false)
  private long totalSkus;

  @Column(name = "total_stock", nullable = false)
  private long totalStock;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  public Long getId() {
    return id;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public LocalDate getReportDate() {
    return reportDate;
  }

  public void setReportDate(LocalDate reportDate) {
    this.reportDate = reportDate;
  }

  public long getTotalSkus() {
    return totalSkus;
  }

  public void setTotalSkus(long totalSkus) {
    this.totalSkus = totalSkus;
  }

  public long getTotalStock() {
    return totalStock;
  }

  public void setTotalStock(long totalStock) {
    this.totalStock = totalStock;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(Instant generatedAt) {
    this.generatedAt = generatedAt;
  }
}

