package com.petsupplies.user.service;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.user.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserLockoutService {
  public static final int MAX_FAILURES = 5;
  public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

  private final UserRepository userRepository;
  private final AuditService auditService;
  private final Clock clock;

  public UserLockoutService(UserRepository userRepository, AuditService auditService, Clock clock) {
    this.userRepository = userRepository;
    this.auditService = auditService;
    this.clock = clock;
  }

  @Transactional
  public void onAuthenticationFailure(String username, String ip) {
    userRepository.findByUsername(username).ifPresent(user -> {
      Instant now = Instant.now(clock);
      if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
        auditService.record(
            "AUTH_LOCKED_ATTEMPT",
            Map.of(
                "username", username,
                "lockedUntil", user.getLockedUntil().toString()
            ),
            username,
            ip
        );
        return;
      }

      int next = user.getFailedAttempts() + 1;
      user.setFailedAttempts(next);

      if (next >= MAX_FAILURES) {
        user.setLockedUntil(now.plus(LOCK_DURATION));
      }

      auditService.record(
          "AUTH_FAILURE",
          buildAuthFailurePayload(username, next, user.getLockedUntil()),
          username,
          ip
      );
    });
  }

  private static Map<String, Object> buildAuthFailurePayload(String username, int failedAttempts, Instant lockedUntil) {
    var payload = new LinkedHashMap<String, Object>();
    payload.put("username", username);
    payload.put("failedAttempts", failedAttempts);
    if (lockedUntil != null) {
      payload.put("lockedUntil", lockedUntil.toString());
    }
    return payload;
  }

  @Transactional
  public void onAuthenticationSuccess(String username, String ip) {
    userRepository.findByUsername(username).ifPresent(user -> {
      boolean changed = false;
      if (user.getFailedAttempts() != 0) {
        user.setFailedAttempts(0);
        changed = true;
      }
      if (user.getLockedUntil() != null) {
        user.setLockedUntil(null);
        changed = true;
      }
      if (changed) {
        auditService.record(
            "AUTH_SUCCESS",
            Map.of("username", username),
            username,
            ip
        );
      } else {
        auditService.record(
            "AUTH_SUCCESS",
            Map.of("username", username),
            username,
            ip
        );
      }
    });
  }

  public static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}

