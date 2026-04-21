package com.petsupplies.reporting.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.reporting.service.ReportingAccessAuditService;
import com.petsupplies.reporting.service.ReportingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only reporting views for {@link com.petsupplies.user.domain.Role#REVIEWER} (compliance / oversight).
 */
@RestController
@RequestMapping("/api/reviewer/reports")
@PreAuthorize("hasRole('REVIEWER')")
public class ReviewerReportingController {
  private final CurrentPrincipal currentPrincipal;
  private final ReportingService reportingService;
  private final ReportingAccessAuditService reportingAccessAuditService;

  public ReviewerReportingController(
      CurrentPrincipal currentPrincipal,
      ReportingService reportingService,
      ReportingAccessAuditService reportingAccessAuditService
  ) {
    this.currentPrincipal = currentPrincipal;
    this.reportingService = reportingService;
    this.reportingAccessAuditService = reportingAccessAuditService;
  }

  @GetMapping("/inventory/{merchantId}")
  public Map<String, Object> inventorySummary(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("merchantId") String merchantId
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    reportingAccessAuditService.record(
        "REVIEWER_REPORT_INVENTORY_SUMMARY_READ",
        user.getUsername(),
        request.getRemoteAddr(),
        Map.of("merchantId", merchantId, "path", request.getRequestURI())
    );
    var row = reportingService.inventorySummary(merchantId);
    return Map.of(
        "merchantId", merchantId,
        "totalSkus", row.getTotalSkus(),
        "totalStock", row.getTotalStock()
    );
  }

  @GetMapping("/inventory/{merchantId}/drill-down")
  public Map<String, Object> drillDown(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("merchantId") String merchantId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    reportingAccessAuditService.record(
        "REVIEWER_REPORT_INVENTORY_DRILLDOWN_READ",
        user.getUsername(),
        request.getRemoteAddr(),
        Map.of("merchantId", merchantId, "page", page, "size", size, "path", request.getRequestURI())
    );
    Page<Map<String, Object>> p = reportingService.inventoryDrillDown(merchantId, page, size);
    return Map.of(
        "content", p.getContent(),
        "page", p.getNumber(),
        "size", p.getSize(),
        "totalElements", p.getTotalElements(),
        "totalPages", p.getTotalPages()
    );
  }
}
