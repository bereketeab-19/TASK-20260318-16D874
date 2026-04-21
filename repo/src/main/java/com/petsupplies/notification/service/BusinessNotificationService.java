package com.petsupplies.notification.service;

import com.petsupplies.notification.domain.Notification;
import com.petsupplies.notification.repo.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessNotificationService {
  public static final String EVENT_ORDER_STATUS = "ORDER_STATUS";
  public static final String EVENT_REVIEW_OUTCOME = "REVIEW_OUTCOME";
  public static final String EVENT_REPORT_HANDLING = "REPORT_HANDLING";

  private final NotificationRepository notificationRepository;
  private final NotificationSubscriptionService notificationSubscriptionService;
  private final Clock clock;

  public BusinessNotificationService(
      NotificationRepository notificationRepository,
      NotificationSubscriptionService notificationSubscriptionService,
      Clock clock
  ) {
    this.notificationRepository = notificationRepository;
    this.notificationSubscriptionService = notificationSubscriptionService;
    this.clock = clock;
  }

  /** @return true if an in-app notification row was persisted */
  @Transactional
  public boolean publishOrderStatus(String merchantId, String orderRef, String status) {
    if (!notificationSubscriptionService.isDeliveryEnabled(merchantId, EVENT_ORDER_STATUS)) {
      return false;
    }
    Instant now = Instant.now(clock);
    Notification n = new Notification();
    n.setMerchantId(merchantId);
    n.setContent("Order " + orderRef + " status: " + status);
    n.setCreatedAt(now);
    n.setDeliveredAt(now);
    n.setEventType(EVENT_ORDER_STATUS);
    notificationRepository.save(n);
    return true;
  }

  /** @return true if an in-app notification row was persisted */
  @Transactional
  public boolean publishReviewOutcome(String merchantId, String reviewRef, String outcome) {
    if (!notificationSubscriptionService.isDeliveryEnabled(merchantId, EVENT_REVIEW_OUTCOME)) {
      return false;
    }
    Instant now = Instant.now(clock);
    Notification n = new Notification();
    n.setMerchantId(merchantId);
    n.setContent("Review " + reviewRef + " outcome: " + outcome);
    n.setCreatedAt(now);
    n.setDeliveredAt(now);
    n.setEventType(EVENT_REVIEW_OUTCOME);
    notificationRepository.save(n);
    return true;
  }

  /** @return true if an in-app notification row was persisted */
  @Transactional
  public boolean publishReportHandling(String merchantId, String detail) {
    if (!notificationSubscriptionService.isDeliveryEnabled(merchantId, EVENT_REPORT_HANDLING)) {
      return false;
    }
    Instant now = Instant.now(clock);
    Notification n = new Notification();
    n.setMerchantId(merchantId);
    n.setContent(detail);
    n.setCreatedAt(now);
    n.setDeliveredAt(now);
    n.setEventType(EVENT_REPORT_HANDLING);
    notificationRepository.save(n);
    return true;
  }
}
