package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petsupplies.auditing.repo.AuditLogRepository;
import com.petsupplies.user.domain.Role;
import com.petsupplies.user.repo.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
class SecurityIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired AuditLogRepository auditLogRepository;
  @Autowired UserRepository userRepository;
  @Autowired MutableClock mutableClock;

  @BeforeEach
  void resetBuyerAndClock() {
    var buyer = userRepository.findByUsername("buyer").orElseThrow();
    buyer.setRole(Role.BUYER);
    buyer.setFailedAttempts(0);
    buyer.setLockedUntil(null);
    userRepository.save(buyer);
    mutableClock.setInstant(Instant.parse("2026-01-01T00:00:00Z"));
  }

  @Test
  void unauthenticated_is_401() throws Exception {
    mockMvc.perform(get("/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void wrong_role_is_403() throws Exception {
    mockMvc.perform(get("/admin/ping").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void buyer_denied_admin_approvals_surface() throws Exception {
    mockMvc.perform(get("/api/admin/approvals/pending").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void buyer_denied_admin_reporting_surface() throws Exception {
    mockMvc.perform(get("/api/admin/reports/inventory/mrc_A").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void reviewer_can_read_reviewer_reporting_surface() throws Exception {
    mockMvc.perform(get("/api/reviewer/reports/inventory/mrc_A").with(httpBasic("reviewer", "buyer123!")))
        .andExpect(status().isOk());
  }

  @Test
  void buyer_denied_reviewer_reporting_surface() throws Exception {
    mockMvc.perform(get("/api/reviewer/reports/inventory/mrc_A").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void lockout_after_5_failures_then_expires() throws Exception {
    long before = auditLogRepository.count();

    for (int i = 0; i < 5; i++) {
      mockMvc.perform(get("/me").with(httpBasic("buyer", "wrong")))
          .andExpect(status().isUnauthorized());
    }

    // Now locked
    mockMvc.perform(get("/me").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isUnauthorized());

    long afterFailures = auditLogRepository.count();
    assertThat(afterFailures).isGreaterThan(before);

    // Advance clock beyond lock window
    mutableClock.setInstant(mutableClock.instant().plusSeconds(16 * 60));

    mockMvc.perform(get("/me").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isOk());
  }

  static class MutableClock extends Clock {
    private final AtomicReference<Instant> now;
    private final ZoneId zoneId;

    MutableClock(Instant initial, ZoneId zoneId) {
      this.now = new AtomicReference<>(initial);
      this.zoneId = zoneId;
    }

    void setInstant(Instant instant) {
      now.set(instant);
    }

    @Override
    public ZoneId getZone() {
      return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return new MutableClock(now.get(), zone);
    }

    @Override
    public Instant instant() {
      return now.get();
    }
  }

  @TestConfiguration
  static class TestClockConfig {
    @Bean
    MutableClock testClock() {
      return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
    }

    @Bean
    @Primary
    Clock clock(MutableClock mutableClock) {
      return mutableClock;
    }
  }
}

