package com.petsupplies.product.repo;

import com.petsupplies.product.domain.Brand;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
  Optional<Brand> findByIdAndMerchantId(Long id, String merchantId);

  List<Brand> findByMerchantIdOrderByNameAsc(String merchantId);

  boolean existsByMerchantIdAndName(String merchantId, String name);
}
