package com.petsupplies.messaging.service;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.messaging.domain.Attachment;
import com.petsupplies.messaging.domain.Message;
import com.petsupplies.messaging.domain.Session;
import com.petsupplies.messaging.repo.AttachmentRepository;
import com.petsupplies.messaging.repo.MessageRepository;
import com.petsupplies.messaging.repo.SessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageService {
  private final SessionRepository sessionRepository;
  private final MessageRepository messageRepository;
  private final AttachmentRepository attachmentRepository;
  private final AntiSpamCache antiSpamCache;
  private final Clock clock;
  private final AuditService auditService;

  public MessageService(
      SessionRepository sessionRepository,
      MessageRepository messageRepository,
      AttachmentRepository attachmentRepository,
      AntiSpamCache antiSpamCache,
      Clock clock,
      AuditService auditService
  ) {
    this.sessionRepository = sessionRepository;
    this.messageRepository = messageRepository;
    this.attachmentRepository = attachmentRepository;
    this.antiSpamCache = antiSpamCache;
    this.clock = clock;
    this.auditService = auditService;
  }

  @Transactional
  public Session createSession(String merchantId) {
    Session s = new Session();
    s.setMerchantId(merchantId);
    s.setCreatedAt(Instant.now(clock));
    return sessionRepository.save(s);
  }

  @Transactional(readOnly = true)
  public List<Session> listSessions(String merchantId) {
    return sessionRepository.findTop50ByMerchantIdOrderByCreatedAtDesc(merchantId);
  }

  @Transactional(readOnly = true)
  public Session requireSession(String merchantId, Long sessionId) {
    return sessionRepository.findByIdAndMerchantId(sessionId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
  }

  @Transactional
  public MessageResult sendText(String merchantId, Long sessionId, String senderUsername, String content) {
    if (content == null || content.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content required");
    }
    Session session = sessionRepository.findByIdAndMerchantId(sessionId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

    String foldedKey = sessionId + ":" + DigestUtils.sha256Hex(content);
    boolean isNew = antiSpamCache.markIfNew(foldedKey);
    if (!isNew) {
      auditService.record(
          "MESSAGE_FOLDED",
          Map.of("merchantId", merchantId, "sessionId", sessionId, "sender", senderUsername),
          senderUsername,
          null
      );
      return new MessageResult(null, true);
    }

    Message m = new Message();
    m.setMerchantId(merchantId);
    m.setSession(session);
    m.setSenderUsername(senderUsername);
    m.setContent(content);
    m.setContentHash(DigestUtils.sha256Hex(content));
    m.setSentAt(Instant.now(clock));
    Message saved = messageRepository.save(m);

    return new MessageResult(saved.getId(), false);
  }

  @Transactional
  public MessageResult sendWithAttachment(String merchantId, Long sessionId, String senderUsername, Attachment attachment) {
    Session session = sessionRepository.findByIdAndMerchantId(sessionId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

    Message m = new Message();
    m.setMerchantId(merchantId);
    m.setSession(session);
    m.setSenderUsername(senderUsername);
    m.setAttachment(attachment);
    m.setSentAt(Instant.now(clock));
    Message saved = messageRepository.save(m);
    return new MessageResult(saved.getId(), false);
  }

  /**
   * Sends a message linked to an attachment already uploaded for this merchant (see {@code POST /attachments}).
   */
  @Transactional
  public MessageResult sendImageMessage(String merchantId, Long sessionId, String senderUsername, long attachmentId, String caption) {
    Attachment attachment = attachmentRepository.findByIdAndMerchantId(attachmentId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "attachment not found"));
    Session session = sessionRepository.findByIdAndMerchantId(sessionId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

    Message m = new Message();
    m.setMerchantId(merchantId);
    m.setSession(session);
    m.setSenderUsername(senderUsername);
    m.setAttachment(attachment);
    if (caption != null && !caption.isBlank()) {
      String t = caption.trim();
      m.setContent(t);
      m.setContentHash(DigestUtils.sha256Hex(t));
    }
    m.setSentAt(Instant.now(clock));
    Message saved = messageRepository.save(m);
    auditService.record(
        "MESSAGE_IMAGE_SENT",
        Map.of(
            "merchantId", merchantId,
            "sessionId", sessionId,
            "attachmentId", attachmentId,
            "messageId", saved.getId()
        ),
        senderUsername,
        null
    );
    return new MessageResult(saved.getId(), false);
  }

  @Transactional
  public void recallMessage(String merchantId, Long sessionId, Long messageId, String senderUsername) {
    Message m = messageRepository.findByIdAndMerchantIdAndSession_Id(messageId, merchantId, sessionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    if (m.getRecalledAt() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already recalled");
    }
    if (!senderUsername.equals(m.getSenderUsername())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only sender can recall");
    }
    m.setRecalledAt(Instant.now(clock));
    m.setContent(null);
    m.setContentHash(null);
    messageRepository.save(m);
    auditService.record(
        "MESSAGE_RECALLED",
        Map.of("merchantId", merchantId, "sessionId", sessionId, "messageId", messageId),
        senderUsername,
        null
    );
  }

  @Transactional
  public void markRead(String merchantId, Long sessionId, Long messageId) {
    Message m = messageRepository.findByIdAndMerchantIdAndSession_Id(messageId, merchantId, sessionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    if (m.getRecalledAt() != null) {
      return;
    }
    if (m.getReadAt() == null) {
      m.setReadAt(Instant.now(clock));
      messageRepository.save(m);
    }
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listSessionMessages(String merchantId, Long sessionId, int page, int size) {
    requireSession(merchantId, sessionId);
    Page<Message> p =
        messageRepository.findByMerchantIdAndSession_IdOrderBySentAtAsc(
            merchantId, sessionId, PageRequest.of(page, size));
    return Map.of(
        "content",
        p.getContent().stream().map(this::toMessageView).toList(),
        "page",
        p.getNumber(),
        "size",
        p.getSize(),
        "totalElements",
        p.getTotalElements(),
        "totalPages",
        p.getTotalPages()
    );
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getSessionMessage(String merchantId, Long sessionId, Long messageId) {
    Message m = messageRepository.findByIdAndMerchantIdAndSession_Id(messageId, merchantId, sessionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    return toMessageView(m);
  }

  private Map<String, Object> toMessageView(Message m) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", m.getId());
    row.put("sessionId", m.getSession().getId());
    row.put("senderUsername", m.getSenderUsername());
    row.put("content", m.getRecalledAt() != null ? null : m.getContent());
    row.put("sentAt", m.getSentAt());
    row.put("readAt", m.getReadAt());
    row.put("recalledAt", m.getRecalledAt());
    if (m.getAttachment() != null) {
      row.put("attachmentId", m.getAttachment().getId());
    }
    return row;
  }

  public record MessageResult(Long messageId, boolean folded) {}
}

