package com.petsupplies.product.repo;

import com.petsupplies.product.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  Optional<Category> findByIdAndMerchantId(Long id, String merchantId);

  List<Category> findByMerchantIdOrderByNameAsc(String merchantId);

  long countByParent_Id(Long parentId);
}
