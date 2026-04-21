package com.petsupplies.notification.repo;

import com.petsupplies.notification.domain.NotificationEventSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventSubscriptionRepository extends JpaRepository<NotificationEventSubscription, Long> {
  List<NotificationEventSubscription> findByMerchantIdOrderByEventTypeAsc(String merchantId);

  Optional<NotificationEventSubscription> findByMerchantIdAndEventType(String merchantId, String eventType);
}
