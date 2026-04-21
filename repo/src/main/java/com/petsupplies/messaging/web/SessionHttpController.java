package com.petsupplies.messaging.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.messaging.service.MessageService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class SessionHttpController {
  private final CurrentPrincipal currentPrincipal;
  private final MessageService messageService;

  public SessionHttpController(CurrentPrincipal currentPrincipal, MessageService messageService) {
    this.currentPrincipal = currentPrincipal;
    this.messageService = messageService;
  }

  @PostMapping("/sessions")
  public Map<String, Object> create(Authentication authentication) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var s = messageService.createSession(merchantId);
    return Map.of("id", s.getId(), "createdAt", s.getCreatedAt());
  }

  @GetMapping("/sessions")
  public List<Map<String, Object>> list(Authentication authentication) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return messageService.listSessions(merchantId).stream()
        .map(s -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", s.getId());
          m.put("createdAt", s.getCreatedAt());
          return m;
        })
        .toList();
  }

  @GetMapping("/sessions/{id}")
  public Map<String, Object> get(Authentication authentication, @PathVariable("id") Long id) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var s = messageService.requireSession(merchantId, id);
    return Map.of("id", s.getId(), "createdAt", s.getCreatedAt());
  }
}
