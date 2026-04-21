package com.petsupplies.cooking.repo;

import com.petsupplies.cooking.domain.TechniqueTag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechniqueTagRepository extends JpaRepository<TechniqueTag, Long> {
  Optional<TechniqueTag> findByMerchantIdAndNameIgnoreCase(String merchantId, String name);
}
