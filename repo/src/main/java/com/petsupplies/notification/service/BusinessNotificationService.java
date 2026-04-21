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
  private final Clock clock;

  public BusinessNotificationService(NotificationRepository notificationRepository, Clock clock) {
    this.notificationRepository = notificationRepository;
    this.clock = clock;
  }

  @Transactional
  public void publishOrderStatus(String merchantId, String orderRef, String status) {
    Instant now = Instant.now(clock);
    Notification n = new Notification();
    n.setMerchantId(merchantId);
    n.setContent("Order " + orderRef + " status: " + status);
    n.setCreatedAt(now);
    n.setDeliveredAt(now);
    n.setEventType(EVENT_ORDER_STATUS);
    notificationRepository.save(n);
  }

  @Transactional
  public void publishReviewOutcome(String merchantId, String reviewRef, String outcome) {
    Instant now = Instant.now(clock);
    Notification n = new Notification();
    n.setMerchantId(merchantId);
    n.setContent("Review " + reviewRef + " outcome: " + outcome);
    n.setCreatedAt(now);
    n.setDeliveredAt(now);
    n.setEventType(EVENT_REVIEW_OUTCOME);
    notificationRepository.save(n);
  }

  @Transactional
  public void publishReportHandling(String merchantId, String detail) {
    Instant now = Instant.now(clock);
    Notification n = new Notification();
    n.setMerchantId(merchantId);
    n.setContent(detail);
    n.setCreatedAt(now);
    n.setDeliveredAt(now);
    n.setEventType(EVENT_REPORT_HANDLING);
    notificationRepository.save(n);
  }
}
