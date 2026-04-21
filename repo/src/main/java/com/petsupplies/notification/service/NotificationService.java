package com.petsupplies.notification.service;

import com.petsupplies.notification.domain.Notification;
import com.petsupplies.notification.repo.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final Clock clock;

  public NotificationService(NotificationRepository notificationRepository, Clock clock) {
    this.notificationRepository = notificationRepository;
    this.clock = clock;
  }

  @Transactional
  public Notification markRead(Long id, String merchantId) {
    Notification n = notificationRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    if (n.getMerchantId() == null || !n.getMerchantId().equals(merchantId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
    }
    n.setReadAt(Instant.now(clock));
    return notificationRepository.save(n);
  }
}
