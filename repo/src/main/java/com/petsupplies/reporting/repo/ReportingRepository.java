package com.petsupplies.reporting.repo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Native SQL projections for static auditability.
 */
public interface ReportingRepository extends org.springframework.data.repository.Repository<com.petsupplies.product.domain.Sku, Long> {

  interface InventorySummaryRow {
    long getTotalSkus();
    long getTotalStock();
  }

  @Query(
      value = "SELECT COUNT(*) AS totalSkus, COALESCE(SUM(stock_quantity),0) AS totalStock " +
          "FROM skus WHERE merchant_id = :merchantId AND active = TRUE",
      nativeQuery = true
  )
  InventorySummaryRow inventorySummary(@Param("merchantId") String merchantId);
}

