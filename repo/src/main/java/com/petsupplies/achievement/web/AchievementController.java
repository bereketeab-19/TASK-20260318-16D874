package com.petsupplies.achievement.web;

import com.petsupplies.achievement.service.AchievementService;
import com.petsupplies.achievement.web.dto.CreateAchievementRequest;
import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.user.security.SecurityUser;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class AchievementController {
  private final CurrentPrincipal currentPrincipal;
  private final AchievementService achievementService;

  public AchievementController(CurrentPrincipal currentPrincipal, AchievementService achievementService) {
    this.currentPrincipal = currentPrincipal;
    this.achievementService = achievementService;
  }

  @PostMapping("/achievements")
  public Map<String, Object> create(Authentication authentication, @Valid @RequestBody CreateAchievementRequest body) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);

    var a = achievementService.create(
        merchantId,
        body.userId(),
        body.title(),
        body.period(),
        body.responsiblePerson(),
        body.conclusion(),
        user.getUsername()
    );

    return Map.of("id", a.getId());
  }

  @GetMapping("/achievements/{id}/export")
  public Map<String, Object> export(
      Authentication authentication,
      @PathVariable("id") Long id,
      @RequestParam(name = "format", defaultValue = "achievement_certificate_v1") String format
  ) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return achievementService.exportJson(merchantId, id, format);
  }

  @PostMapping(value = "/achievements/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> upload(
      Authentication authentication,
      @PathVariable("id") Long achievementId,
      @RequestPart("file") MultipartFile file
  ) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);

    var v = achievementService.addAttachmentVersion(merchantId, achievementId, file, user.getUsername());
    return Map.of("id", v.getId(), "version", v.getVersion(), "sha256", v.getSha256());
  }
}

