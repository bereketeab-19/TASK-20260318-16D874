package com.petsupplies.messaging.web;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.messaging.service.AttachmentService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class AttachmentController {
  private final CurrentPrincipal currentPrincipal;
  private final AttachmentService attachmentService;
  private final AuditService auditService;

  public AttachmentController(CurrentPrincipal currentPrincipal, AttachmentService attachmentService, AuditService auditService) {
    this.currentPrincipal = currentPrincipal;
    this.attachmentService = attachmentService;
    this.auditService = auditService;
  }

  @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> upload(Authentication authentication, HttpServletRequest request, @RequestPart("file") MultipartFile file) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var a = attachmentService.store(merchantId, file);

    auditService.record(
        "ATTACHMENT_UPLOADED",
        Map.of("merchantId", merchantId, "attachmentId", a.getId(), "sha256", a.getSha256(), "size", a.getSizeBytes()),
        user.getUsername(),
        request.getRemoteAddr()
    );

    return Map.of("id", a.getId(), "sha256", a.getSha256(), "contentType", a.getContentType());
  }
}

