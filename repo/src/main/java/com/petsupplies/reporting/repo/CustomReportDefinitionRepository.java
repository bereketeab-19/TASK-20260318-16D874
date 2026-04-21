package com.petsupplies.reporting.repo;

import com.petsupplies.reporting.domain.CustomReportDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomReportDefinitionRepository extends JpaRepository<CustomReportDefinition, Long> {
  Optional<CustomReportDefinition> findByIdAndMerchantId(Long id, String merchantId);

  List<CustomReportDefinition> findByMerchantIdOrderByNameAsc(String merchantId);
}
