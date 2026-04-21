package com.petsupplies.user.service;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.user.repo.UserRepository;
import com.petsupplies.user.security.SecurityUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;
  private final Clock clock;
  private final AuditService auditService;

  public DbUserDetailsService(UserRepository userRepository, Clock clock, AuditService auditService) {
    this.userRepository = userRepository;
    this.clock = clock;
    this.auditService = auditService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now(clock))) {
      auditService.record(
          "AUTH_LOCKED",
          Map.of("username", username, "lockedUntil", user.getLockedUntil().toString()),
          username,
          null
      );
      throw new LockedException("Account locked");
    }

    return new SecurityUser(
        user.getId(),
        user.getUsername(),
        user.getPasswordHash(),
        user.getMerchantId(),
        java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
    );
  }
}

