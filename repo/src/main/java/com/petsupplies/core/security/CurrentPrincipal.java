package com.petsupplies.core.security;

import com.petsupplies.user.security.SecurityUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentPrincipal {
  public SecurityUser requireSecurityUser(Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof SecurityUser su) {
      return su;
    }
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
  }

  public String requireMerchantId(Authentication authentication) {
    SecurityUser su = requireSecurityUser(authentication);
    if (su.getMerchantId() == null || su.getMerchantId().isBlank()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Merchant scope required");
    }
    return su.getMerchantId();
  }

  /** Explicit role guard for merchant business APIs (complements tenant scope checks). */
  public void requireMerchantRole(Authentication authentication) {
    requireSecurityUser(authentication);
    if (authentication.getAuthorities().stream().noneMatch(a -> "ROLE_MERCHANT".equals(a.getAuthority()))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Merchant role required");
    }
  }
}

