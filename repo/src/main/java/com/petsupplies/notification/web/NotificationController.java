package com.petsupplies.notification.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.notification.repo.NotificationRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.petsupplies.notification.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class NotificationController {
  private final CurrentPrincipal currentPrincipal;
  private final NotificationRepository notificationRepository;
  private final NotificationService notificationService;

  public NotificationController(
      CurrentPrincipal currentPrincipal,
      NotificationRepository notificationRepository,
      NotificationService notificationService
  ) {
    this.currentPrincipal = currentPrincipal;
    this.notificationRepository = notificationRepository;
    this.notificationService = notificationService;
  }

  @GetMapping("/notifications")
  public List<Map<String, Object>> list(Authentication authentication) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return notificationRepository.findTop100ByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
        .map(n -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", n.getId());
          m.put("content", n.getContent());
          m.put("createdAt", n.getCreatedAt());
          m.put("deliveredAt", n.getDeliveredAt());
          m.put("eventType", n.getEventType());
          m.put("readAt", n.getReadAt());
          return m;
        })
        .toList();
  }

  @PatchMapping("/notifications/{id}/read")
  public Map<String, Object> markRead(Authentication authentication, @PathVariable("id") Long id) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var n = notificationService.markRead(id, merchantId);
    return Map.of("id", n.getId(), "readAt", n.getReadAt());
  }
}

