package com.petsupplies.notification.service;

import com.petsupplies.notification.domain.NotificationEventSubscription;
import com.petsupplies.notification.repo.NotificationEventSubscriptionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationSubscriptionService {
  /** Known internal event types for preference rows (extend as new types are introduced). */
  public static final List<String> KNOWN_EVENT_TYPES = List.of(
      "LOW_STOCK",
      "ORDER_STATUS",
      "REVIEW_OUTCOME",
      "REPORT_HANDLING"
  );

  private final NotificationEventSubscriptionRepository subscriptionRepository;
  private final Clock clock;

  public NotificationSubscriptionService(
      NotificationEventSubscriptionRepository subscriptionRepository,
      Clock clock
  ) {
    this.subscriptionRepository = subscriptionRepository;
    this.clock = clock;
  }

  /**
   * Whether an in-app notification row should be created for this merchant and event type.
   * Unknown event types default to <strong>enabled</strong> (forward-compatible). Known types default to
   * enabled when no preference row exists.
   */
  @Transactional(readOnly = true)
  public boolean isDeliveryEnabled(String merchantId, String eventType) {
    if (merchantId == null || merchantId.isBlank()) {
      return true;
    }
    if (eventType == null || eventType.isBlank()) {
      return true;
    }
    String et = eventType.trim();
    if (KNOWN_EVENT_TYPES.stream().noneMatch(et::equals)) {
      return true;
    }
    return subscriptionRepository
        .findByMerchantIdAndEventType(merchantId, et)
        .map(NotificationEventSubscription::isEnabled)
        .orElse(true);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listPreferences(String merchantId) {
    var saved = subscriptionRepository.findByMerchantIdOrderByEventTypeAsc(merchantId);
    Map<String, Boolean> map = new LinkedHashMap<>();
    for (var s : saved) {
      map.put(s.getEventType(), s.isEnabled());
    }
    List<Map<String, Object>> out = new ArrayList<>();
    for (String et : KNOWN_EVENT_TYPES) {
      out.add(Map.of(
          "eventType", et,
          "enabled", map.getOrDefault(et, Boolean.TRUE)
      ));
    }
    return out;
  }

  @Transactional
  public Map<String, Object> upsert(String merchantId, String eventType, boolean enabled) {
    String et = eventType == null ? "" : eventType.trim();
    if (et.isEmpty() || KNOWN_EVENT_TYPES.stream().noneMatch(et::equals)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "Unknown or unsupported eventType"
      );
    }
    Instant now = Instant.now(clock);
    NotificationEventSubscription row =
        subscriptionRepository.findByMerchantIdAndEventType(merchantId, et).orElseGet(() -> {
          NotificationEventSubscription n = new NotificationEventSubscription();
          n.setMerchantId(merchantId);
          n.setEventType(et);
          n.setCreatedAt(now);
          return n;
        });
    row.setEnabled(enabled);
    row.setUpdatedAt(now);
    subscriptionRepository.save(row);
    return Map.of("eventType", et, "enabled", enabled);
  }
}
