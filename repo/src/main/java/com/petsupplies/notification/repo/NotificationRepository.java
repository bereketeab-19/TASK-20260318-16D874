package com.petsupplies.notification.repo;

import com.petsupplies.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findTop100ByMerchantIdOrderByCreatedAtDesc(String merchantId);

  long countByMerchantId(String merchantId);

  @Query(
      "SELECT n.eventType, COUNT(n) FROM Notification n WHERE n.merchantId = :merchantId "
          + "AND n.createdAt >= :since GROUP BY n.eventType"
  )
  List<Object[]> countByEventTypeSince(
      @Param("merchantId") String merchantId,
      @Param("since") Instant since
  );
}

