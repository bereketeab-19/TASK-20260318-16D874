package com.petsupplies.cooking.repo;

import com.petsupplies.cooking.domain.CookingStep;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CookingStepRepository extends JpaRepository<CookingStep, Long> {
  List<CookingStep> findAllByProcess_IdOrderByStepIndexAsc(Long processId);

  Optional<CookingStep> findByIdAndProcess_MerchantId(Long id, String merchantId);

  @Query("SELECT COALESCE(MAX(s.stepIndex), -1) FROM CookingStep s WHERE s.process.id = :processId")
  int maxStepIndex(@Param("processId") Long processId);
}

