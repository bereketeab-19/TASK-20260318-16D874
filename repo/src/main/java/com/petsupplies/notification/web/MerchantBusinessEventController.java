package com.petsupplies.notification.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.notification.service.BusinessNotificationService;
import com.petsupplies.notification.web.dto.ReviewOutcomeEventRequest;
import com.petsupplies.notification.web.dto.OrderStatusEventRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Merchant-scoped hooks to emit internal notifications for order/review lifecycle events.
 */
@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantBusinessEventController {
  private final CurrentPrincipal currentPrincipal;
  private final BusinessNotificationService businessNotificationService;

  public MerchantBusinessEventController(
      CurrentPrincipal currentPrincipal,
      BusinessNotificationService businessNotificationService
  ) {
    this.currentPrincipal = currentPrincipal;
    this.businessNotificationService = businessNotificationService;
  }

  @PostMapping("/api/merchant/events/order-status")
  public Map<String, Object> orderStatus(Authentication authentication, @Valid @RequestBody OrderStatusEventRequest body) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    businessNotificationService.publishOrderStatus(merchantId, body.orderRef(), body.status());
    return Map.of("dispatched", true, "eventType", BusinessNotificationService.EVENT_ORDER_STATUS);
  }

  @PostMapping("/api/merchant/events/review-outcome")
  public Map<String, Object> reviewOutcome(Authentication authentication, @Valid @RequestBody ReviewOutcomeEventRequest body) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    businessNotificationService.publishReviewOutcome(merchantId, body.reviewRef(), body.outcome());
    return Map.of("dispatched", true, "eventType", BusinessNotificationService.EVENT_REVIEW_OUTCOME);
  }
}
