package com.petsupplies.messaging.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.messaging.service.MessageService;
import com.petsupplies.messaging.web.dto.SendImageMessageHttpRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class MessagingHttpController {
  private final CurrentPrincipal currentPrincipal;
  private final MessageService messageService;

  public MessagingHttpController(CurrentPrincipal currentPrincipal, MessageService messageService) {
    this.currentPrincipal = currentPrincipal;
    this.messageService = messageService;
  }

  /** Paginated message history for the session (includes {@code readAt} / {@code recalledAt}). */
  @GetMapping("/sessions/{sessionId}/messages")
  public Map<String, Object> listMessages(
      Authentication authentication,
      @PathVariable("sessionId") Long sessionId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size
  ) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return messageService.listSessionMessages(merchantId, sessionId, page, size);
  }

  @GetMapping("/sessions/{sessionId}/messages/{messageId}")
  public Map<String, Object> getMessage(
      Authentication authentication,
      @PathVariable("sessionId") Long sessionId,
      @PathVariable("messageId") Long messageId
  ) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return messageService.getSessionMessage(merchantId, sessionId, messageId);
  }

  @PostMapping("/sessions/{sessionId}/messages/{messageId}/recall")
  public Map<String, Object> recall(
      Authentication authentication,
      @PathVariable("sessionId") Long sessionId,
      @PathVariable("messageId") Long messageId
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    messageService.recallMessage(merchantId, sessionId, messageId, user.getUsername());
    return Map.of("recalled", true);
  }

  @PostMapping("/sessions/{sessionId}/messages/{messageId}/read")
  public Map<String, Object> markRead(
      Authentication authentication,
      @PathVariable("sessionId") Long sessionId,
      @PathVariable("messageId") Long messageId
  ) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    messageService.markRead(merchantId, sessionId, messageId);
    return Map.of("read", true);
  }

  @PostMapping("/sessions/{sessionId}/messages/image")
  public Map<String, Object> sendImage(
      Authentication authentication,
      @PathVariable("sessionId") Long sessionId,
      @Valid @RequestBody SendImageMessageHttpRequest body
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var result = messageService.sendImageMessage(merchantId, sessionId, user.getUsername(), body.attachmentId(), body.caption());
    return Map.of("messageId", result.messageId(), "folded", result.folded());
  }
}
