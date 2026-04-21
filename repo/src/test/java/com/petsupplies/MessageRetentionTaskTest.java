package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.messaging.repo.MessageRepository;
import com.petsupplies.scheduling.MessageRetentionTask;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageRetentionTaskTest {

  @Mock MessageRepository messageRepository;
  @Mock AuditService auditService;
  @Mock Clock clock;

  @InjectMocks MessageRetentionTask messageRetentionTask;

  @Test
  void purge_deletes_older_than_180_days_and_audits() {
    Instant now = Instant.parse("2026-06-01T02:00:00Z");
    when(clock.instant()).thenReturn(now);
    when(messageRepository.deleteOlderThan(any())).thenReturn(7);

    messageRetentionTask.purgeOldMessages();

    ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
    verify(messageRepository).deleteOlderThan(cutoff.capture());
    assertThat(cutoff.getValue()).isEqualTo(now.minus(180, ChronoUnit.DAYS));

    verify(auditService).record(eq("MESSAGE_RETENTION_PURGE"), anyMap(), eq("system"), isNull());
  }
}
