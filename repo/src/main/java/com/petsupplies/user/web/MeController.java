package com.petsupplies.user.web;

import com.petsupplies.user.security.SecurityUser;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {
  @GetMapping("/me")
  public Map<String, Object> me(Authentication authentication) {
    String merchantId = null;
    if (authentication.getPrincipal() instanceof SecurityUser su) {
      merchantId = su.getMerchantId();
    }
    var body = new LinkedHashMap<String, Object>();
    body.put("username", authentication.getName());
    body.put("authorities", authentication.getAuthorities().stream().map(a -> a.getAuthority()).sorted().toList());
    body.put("merchantId", merchantId);
    return body;
  }
}

