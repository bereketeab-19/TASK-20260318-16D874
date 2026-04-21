package com.petsupplies.product.repo;

import com.petsupplies.product.domain.Sku;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkuRepository extends JpaRepository<Sku, Long> {
  Optional<Sku> findByIdAndMerchantId(Long id, String merchantId);
  List<Sku> findAllByMerchantId(String merchantId);

  List<Sku> findByMerchantIdAndActiveTrueOrderByIdDesc(String merchantId);

  Page<Sku> findByMerchantIdAndActiveTrueOrderByIdDesc(String merchantId, Pageable pageable);

  long countByMerchantId(String merchantId);

  long countByMerchantIdAndActiveTrue(String merchantId);

  @Modifying
  @Query("UPDATE Sku s SET s.stockQuantity = :qty WHERE s.id = :id AND s.merchantId = :merchantId")
  int updateStockScoped(@Param("id") Long id, @Param("qty") int qty, @Param("merchantId") String merchantId);
}

