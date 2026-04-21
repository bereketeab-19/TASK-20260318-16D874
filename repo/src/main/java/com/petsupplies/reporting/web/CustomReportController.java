package com.petsupplies.reporting.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.reporting.service.CustomReportService;
import com.petsupplies.reporting.service.ReportingAccessAuditService;
import com.petsupplies.reporting.web.dto.CreateCustomReportRequest;
import com.petsupplies.reporting.web.dto.UpdateCustomReportRequest;
import com.petsupplies.user.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class CustomReportController {
  private final CurrentPrincipal currentPrincipal;
  private final CustomReportService customReportService;
  private final ReportingAccessAuditService reportingAccessAuditService;

  public CustomReportController(
      CurrentPrincipal currentPrincipal,
      CustomReportService customReportService,
      ReportingAccessAuditService reportingAccessAuditService
  ) {
    this.currentPrincipal = currentPrincipal;
    this.customReportService = customReportService;
    this.reportingAccessAuditService = reportingAccessAuditService;
  }

  @GetMapping("/merchant/custom-reports")
  public List<Map<String, Object>> list(Authentication authentication, HttpServletRequest request) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    reportingAccessAuditService.record(
        "MERCHANT_CUSTOM_REPORT_LIST_READ",
        user.getUsername(),
        request.getRemoteAddr(),
        Map.of("merchantId", merchantId, "path", request.getRequestURI())
    );
    return customReportService.listRows(merchantId);
  }

  @PostMapping("/merchant/custom-reports")
  public Map<String, Object> create(Authentication authentication, @Valid @RequestBody CreateCustomReportRequest body) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return customReportService.create(merchantId, user.getUserId(), user.getUsername(), body);
  }

  @GetMapping("/merchant/custom-reports/{id}")
  public Map<String, Object> get(Authentication authentication, HttpServletRequest request, @PathVariable("id") Long id) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    reportingAccessAuditService.record(
        "MERCHANT_CUSTOM_REPORT_GET_READ",
        user.getUsername(),
        request.getRemoteAddr(),
        Map.of("merchantId", merchantId, "reportDefinitionId", id, "path", request.getRequestURI())
    );
    return customReportService.toRow(customReportService.requireScoped(id, merchantId));
  }

  @PatchMapping("/merchant/custom-reports/{id}")
  public Map<String, Object> update(
      Authentication authentication,
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateCustomReportRequest body
  ) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return customReportService.update(merchantId, id, user.getUsername(), body);
  }

  @DeleteMapping("/merchant/custom-reports/{id}")
  public Map<String, Object> delete(Authentication authentication, @PathVariable("id") Long id) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    customReportService.delete(merchantId, id, user.getUsername());
    return Map.of("deleted", true);
  }

  @PostMapping("/merchant/custom-reports/{id}/execute")
  public Map<String, Object> execute(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("id") Long id
  ) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return customReportService.execute(merchantId, id, user.getUsername(), request.getRemoteAddr());
  }
}
