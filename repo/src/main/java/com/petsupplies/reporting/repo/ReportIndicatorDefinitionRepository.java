package com.petsupplies.reporting.repo;

import com.petsupplies.reporting.domain.ReportIndicatorDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportIndicatorDefinitionRepository extends JpaRepository<ReportIndicatorDefinition, Long> {
  List<ReportIndicatorDefinition> findAllByOrderByCodeAsc();
}
