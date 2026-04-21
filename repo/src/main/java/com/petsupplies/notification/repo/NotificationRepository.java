package com.petsupplies.notification.repo;

import com.petsupplies.notification.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findTop100ByMerchantIdOrderByCreatedAtDesc(String merchantId);
}

