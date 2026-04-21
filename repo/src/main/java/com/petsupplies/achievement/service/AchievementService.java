package com.petsupplies.achievement.service;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.achievement.domain.Achievement;
import com.petsupplies.achievement.domain.AchievementAttachmentVersion;
import com.petsupplies.achievement.repo.AchievementAttachmentVersionRepository;
import com.petsupplies.achievement.repo.AchievementRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AchievementService {
  private final AchievementRepository achievementRepository;
  private final AchievementAttachmentVersionRepository attachmentVersionRepository;
  private final AuditService auditService;
  private final Clock clock;
  private final Path attachmentsDir;

  public AchievementService(
      AchievementRepository achievementRepository,
      AchievementAttachmentVersionRepository attachmentVersionRepository,
      AuditService auditService,
      Clock clock,
      @Value("${app.achievementAttachments.dir}") String attachmentsDir
  ) {
    this.achievementRepository = achievementRepository;
    this.attachmentVersionRepository = attachmentVersionRepository;
    this.auditService = auditService;
    this.clock = clock;
    this.attachmentsDir = Path.of(attachmentsDir);
  }

  @Transactional
  public Achievement create(
      String merchantId,
      Long userId,
      String title,
      String period,
      String responsiblePerson,
      String conclusion,
      String actorUsername
  ) {
    if (responsiblePerson == null || responsiblePerson.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "responsible_person required");
    }
    if (conclusion == null || conclusion.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conclusion required");
    }

    Achievement a = new Achievement();
    a.setMerchantId(merchantId);
    a.setUserId(userId);
    a.setTitle(title);
    a.setPeriod(period);
    a.setResponsiblePerson(responsiblePerson);
    a.setConclusion(conclusion);
    a.setCreatedAt(Instant.now(clock));
    Achievement saved = achievementRepository.save(a);

    auditService.record(
        "ACHIEVEMENT_CREATED",
        Map.of("merchantId", merchantId, "achievementId", saved.getId()),
        actorUsername,
        null
    );

    return saved;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> exportJson(String merchantId, Long achievementId, String format) {
    Achievement a = achievementRepository.findByIdAndMerchantId(achievementId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    String f = format == null || format.isBlank() ? "achievement_certificate_v1" : format.trim();
    if ("achievement_certificate_v1".equals(f)) {
      return Map.of(
          "format", "achievement_certificate_v1",
          "achievementId", a.getId(),
          "merchantId", a.getMerchantId(),
          "userId", a.getUserId(),
          "title", a.getTitle(),
          "period", a.getPeriod(),
          "responsiblePerson", a.getResponsiblePerson(),
          "conclusion", a.getConclusion(),
          "createdAt", a.getCreatedAt()
      );
    }
    if ("achievement_assessment_form_v1".equals(f)) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("format", "achievement_assessment_form_v1");
      m.put("achievementId", a.getId());
      m.put("merchantId", a.getMerchantId());
      m.put("userId", a.getUserId());
      m.put("schemaVersion", 1);
      m.put("sections", List.of(
          Map.of(
              "id", "summary",
              "title", "Practice summary",
              "fields",
              List.of(
                  Map.of("key", "title", "value", a.getTitle()),
                  Map.of("key", "period", "value", a.getPeriod() == null ? "" : a.getPeriod()),
                  Map.of("key", "responsiblePerson", "value", a.getResponsiblePerson()),
                  Map.of("key", "conclusion", "value", a.getConclusion())
              )
          ),
          Map.of(
              "id", "assessment",
              "title", "Structured assessment",
              "fields",
              List.of(
                  Map.of("key", "outcomeSummary", "label", "Outcome", "value", a.getConclusion()),
                  Map.of("key", "recordedAt", "label", "Recorded at", "value", a.getCreatedAt().toString())
              )
          )
      ));
      return m;
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown export format: " + f);
  }

  @Transactional
  public AchievementAttachmentVersion addAttachmentVersion(
      String merchantId,
      Long achievementId,
      MultipartFile file,
      String actorUsername
  ) {
    Achievement achievement = achievementRepository.findByIdAndMerchantId(achievementId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
    }

    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read file");
    }

    String sha256 = DigestUtils.sha256Hex(bytes);
    String ct = file.getContentType() == null ? "application/octet-stream" : file.getContentType();

    Integer max = attachmentVersionRepository.findMaxVersion(achievementId);
    int nextVersion = (max == null ? 1 : (max + 1));

    String filename = "achievement_" + achievementId + "_v" + nextVersion + "_" + sha256;
    Path target = attachmentsDir.resolve(filename).normalize();
    try {
      Files.createDirectories(attachmentsDir);
      if (!target.startsWith(attachmentsDir)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attachment path");
      }
      Files.write(target, bytes);
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store attachment");
    }

    AchievementAttachmentVersion v = new AchievementAttachmentVersion();
    v.setAchievement(achievement);
    v.setVersion(nextVersion);
    v.setSha256(sha256);
    v.setContentType(ct);
    v.setSizeBytes(bytes.length);
    v.setStoragePath(target.toString());
    v.setCreatedAt(Instant.now(clock));

    AchievementAttachmentVersion saved = attachmentVersionRepository.save(v);

    auditService.record(
        "ACHIEVEMENT_VERSION_INCREMENTED",
        Map.of("merchantId", merchantId, "achievementId", achievementId, "version", nextVersion, "sha256", sha256),
        actorUsername,
        null
    );

    return saved;
  }
}

