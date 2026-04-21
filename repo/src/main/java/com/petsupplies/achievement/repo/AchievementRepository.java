package com.petsupplies.achievement.repo;

import com.petsupplies.achievement.domain.Achievement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
  Optional<Achievement> findByIdAndMerchantId(Long id, String merchantId);
}

