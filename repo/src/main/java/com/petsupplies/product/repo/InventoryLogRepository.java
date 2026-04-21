package com.petsupplies.product.repo;

import com.petsupplies.product.domain.InventoryLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

  Page<InventoryLog> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

  Page<InventoryLog> findByMerchantIdAndSku_IdOrderByCreatedAtDesc(
      String merchantId,
      Long skuId,
      Pageable pageable
  );

  @Query(
      value = "SELECT DATE(created_at) AS dayBucket, "
          + "CAST(SUM(ABS(quantity_after - quantity_before)) AS SIGNED) AS movementUnits, "
          + "COUNT(*) AS eventCount "
          + "FROM inventory_logs WHERE merchant_id = :merchantId "
          + "AND created_at >= :fromInc AND created_at < :toExc "
          + "GROUP BY DATE(created_at) ORDER BY dayBucket",
      nativeQuery = true
  )
  List<Object[]> sumMovementByDay(
      @Param("merchantId") String merchantId,
      @Param("fromInc") Instant fromInc,
      @Param("toExc") Instant toExc
  );
}
