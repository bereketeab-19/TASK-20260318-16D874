package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;

import com.petsupplies.messaging.repo.MessageRepository;
import com.petsupplies.messaging.service.MessageService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

class MessagingAntiSpamIntegrationTest extends AbstractIntegrationTest {

  @Autowired MessageService messageService;
  @Autowired MessageRepository messageRepository;
  @Autowired MutableClock mutableClock;

  @Test
  void duplicate_text_within_10_seconds_is_folded_not_persisted() {
    var session = messageService.createSession("mrc_A");

    var r1 = messageService.sendText("mrc_A", session.getId(), "merchantA", "Hello");
    assertThat(r1.folded()).isFalse();

    mutableClock.setInstant(mutableClock.instant().plusSeconds(5));
    var r2 = messageService.sendText("mrc_A", session.getId(), "merchantA", "Hello");
    assertThat(r2.folded()).isTrue();

    assertThat(messageRepository.countByMerchantIdAndSession_Id("mrc_A", session.getId())).isEqualTo(1);
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

