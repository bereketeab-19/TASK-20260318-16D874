package com.petsupplies.cooking.repo;

import com.petsupplies.cooking.domain.CookingProcess;
import com.petsupplies.cooking.domain.CookingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CookingProcessRepository extends JpaRepository<CookingProcess, Long> {
  Optional<CookingProcess> findByIdAndMerchantId(Long id, String merchantId);

  List<CookingProcess> findAllByStatus(CookingStatus status);
}

