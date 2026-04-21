package com.petsupplies.auditing.web;

import com.petsupplies.auditing.repo.PendingApprovalRepository;
import com.petsupplies.auditing.service.PendingApprovalService;
import com.petsupplies.auditing.web.dto.ApprovalRequestDto;
import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.user.security.SecurityUser;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class ApprovalController {
  private final CurrentPrincipal currentPrincipal;
  private final PendingApprovalService pendingApprovalService;
  private final PendingApprovalRepository pendingApprovalRepository;

  public ApprovalController(
      CurrentPrincipal currentPrincipal,
      PendingApprovalService pendingApprovalService,
      PendingApprovalRepository pendingApprovalRepository
  ) {
    this.currentPrincipal = currentPrincipal;
    this.pendingApprovalService = pendingApprovalService;
    this.pendingApprovalRepository = pendingApprovalRepository;
  }

  @PostMapping("/api/admin/approvals/request")
  public Map<String, Object> request(Authentication authentication, @Valid @RequestBody ApprovalRequestDto body) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    var created = pendingApprovalService.request(body.operationType(), body.payload(), user.getUserId(), user.getUsername());
    return Map.of("id", created.getId(), "status", created.getStatus().name());
  }

  @PostMapping("/api/admin/approvals/{id}/execute")
  public Map<String, Object> execute(Authentication authentication, @PathVariable("id") Long id) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    var executed = pendingApprovalService.execute(id, user.getUserId(), user.getUsername());
    return Map.of("id", executed.getId(), "status", executed.getStatus().name());
  }

  @PostMapping("/api/admin/approvals/{id}/reject")
  public Map<String, Object> reject(Authentication authentication, @PathVariable("id") Long id) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    var rejected = pendingApprovalService.reject(id, user.getUserId(), user.getUsername());
    return Map.of("id", rejected.getId(), "status", rejected.getStatus().name());
  }

  @GetMapping("/api/admin/approvals/pending")
  public Object listPending() {
    return pendingApprovalRepository.findTop100ByStatusOrderByCreatedAtDesc(com.petsupplies.auditing.domain.PendingApprovalStatus.PENDING)
        .stream()
        .map(p -> Map.of(
            "id", p.getId(),
            "operationType", p.getOperationType().name(),
            "status", p.getStatus().name(),
            "requesterUserId", p.getRequesterUserId(),
            "requesterUsername", p.getRequesterUsername(),
            "createdAt", p.getCreatedAt()
        ))
        .toList();
  }
}

