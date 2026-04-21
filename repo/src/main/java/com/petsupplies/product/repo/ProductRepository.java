package com.petsupplies.product.repo;

import com.petsupplies.product.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {
  Optional<Product> findByIdAndMerchantId(Long id, String merchantId);
  Optional<Product> findByProductCodeAndMerchantId(String productCode, String merchantId);

  long countByMerchantId(String merchantId);

  long countByMerchantIdAndActiveTrue(String merchantId);

  List<Product> findByMerchantIdAndActiveTrueOrderByCreatedAtDesc(String merchantId);

  long countByCategory_Id(Long categoryId);

  long countByBrand_Id(Long brandId);

  Page<Product> findByActiveTrueOrderByIdDesc(Pageable pageable);

  long countByActiveTrue();

  @Query("select count(distinct p.merchantId) from Product p where p.active = true")
  long countDistinctMerchantIdByActiveTrue();
}

