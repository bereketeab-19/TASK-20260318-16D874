package com.petsupplies.cooking.repo;

import com.petsupplies.cooking.domain.CookingTimer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CookingTimerRepository extends JpaRepository<CookingTimer, Long> {
  List<CookingTimer> findAllByProcess_Id(Long processId);
}

