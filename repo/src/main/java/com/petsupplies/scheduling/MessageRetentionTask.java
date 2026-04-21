package com.petsupplies.scheduling;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.messaging.repo.MessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MessageRetentionTask {
  private final MessageRepository messageRepository;
  private final Clock clock;
  private final AuditService auditService;

  public MessageRetentionTask(MessageRepository messageRepository, Clock clock, AuditService auditService) {
    this.messageRepository = messageRepository;
    this.clock = clock;
    this.auditService = auditService;
  }

  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void purgeOldMessages() {
    Instant cutoff = Instant.now(clock).minus(180, ChronoUnit.DAYS);
    int deleted = messageRepository.deleteOlderThan(cutoff);
    auditService.record(
        "MESSAGE_RETENTION_PURGE",
        Map.of("cutoff", cutoff.toString(), "deletedCount", deleted),
        "system",
        null
    );
  }
}

