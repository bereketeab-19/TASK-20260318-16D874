package com.petsupplies.messaging.web;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.messaging.service.MessageService;
import com.petsupplies.messaging.web.dto.SendImageMessage;
import com.petsupplies.messaging.web.dto.SendTextMessage;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class MessagingController {
  private final CurrentPrincipal currentPrincipal;
  private final MessageService messageService;
  private final SimpMessagingTemplate messagingTemplate;
  private final AuditService auditService;

  public MessagingController(
      CurrentPrincipal currentPrincipal,
      MessageService messageService,
      SimpMessagingTemplate messagingTemplate,
      AuditService auditService
  ) {
    this.currentPrincipal = currentPrincipal;
    this.messageService = messageService;
    this.messagingTemplate = messagingTemplate;
    this.auditService = auditService;
  }

  /**
   * Creates a chat session and broadcasts lifecycle on {@code /topic/sessions.{merchantId}.lifecycle}
   * (same scope rules as {@link com.petsupplies.messaging.web.SessionHttpController#create}).
   */
  @MessageMapping("/sessions.create")
  public void createSession(Authentication authentication) {
    currentPrincipal.requireMerchantRole(authentication);
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var session = messageService.createSession(merchantId);
    Map<String, Object> lifecycle = Map.of(
        "event", "SESSION_CREATED",
        "sessionId", session.getId(),
        "createdAt", session.getCreatedAt(),
        "createdBy", user.getUsername()
    );
    messagingTemplate.convertAndSend("/topic/sessions." + merchantId + ".lifecycle", lifecycle);
    auditService.record(
        "SESSION_CREATED_STOMP",
        Map.of("merchantId", merchantId, "sessionId", session.getId()),
        user.getUsername(),
        null
    );
  }

  @MessageMapping("/messages.sendText")
  public void sendText(Authentication authentication, @Valid @Payload SendTextMessage payload) {
    currentPrincipal.requireMerchantRole(authentication);
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);

    var result = messageService.sendText(merchantId, payload.sessionId(), user.getUsername(), payload.content());
    Map<String, Object> event = Map.of(
        "sessionId", payload.sessionId(),
        "sender", user.getUsername(),
        "content", payload.content(),
        "folded", result.folded(),
        "messageId", result.messageId()
    );
    messagingTemplate.convertAndSend("/topic/messages." + merchantId + "." + payload.sessionId(), event);
  }

  @MessageMapping("/messages.sendImage")
  public void sendImage(Authentication authentication, @Valid @Payload SendImageMessage payload) {
    currentPrincipal.requireMerchantRole(authentication);
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);

    var result = messageService.sendImageMessage(
        merchantId,
        payload.sessionId(),
        user.getUsername(),
        payload.attachmentId(),
        payload.caption()
    );
    Map<String, Object> event = Map.of(
        "sessionId", payload.sessionId(),
        "sender", user.getUsername(),
        "attachmentId", payload.attachmentId(),
        "caption", payload.caption() != null ? payload.caption() : "",
        "folded", result.folded(),
        "messageId", result.messageId()
    );
    messagingTemplate.convertAndSend("/topic/messages." + merchantId + "." + payload.sessionId(), event);
  }
}

