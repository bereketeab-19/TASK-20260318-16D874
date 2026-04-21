package com.petsupplies.product.repo;

import com.petsupplies.product.domain.AttributeDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {
  List<AttributeDefinition> findByMerchantIdOrderByCodeAsc(String merchantId);

  Optional<AttributeDefinition> findByIdAndMerchantId(Long id, String merchantId);
}
