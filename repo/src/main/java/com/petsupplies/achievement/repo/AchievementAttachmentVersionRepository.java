package com.petsupplies.achievement.repo;

import com.petsupplies.achievement.domain.AchievementAttachmentVersion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AchievementAttachmentVersionRepository extends JpaRepository<AchievementAttachmentVersion, Long> {
  @Query("SELECT MAX(a.version) FROM AchievementAttachmentVersion a WHERE a.achievement.id = :achievementId")
  Integer findMaxVersion(@Param("achievementId") Long achievementId);

  Optional<AchievementAttachmentVersion> findByAchievement_IdAndVersion(Long achievementId, int version);
}

