package com.petsupplies.reporting.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.reporting.service.ReportingAccessAuditService;
import com.petsupplies.reporting.service.ReportingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class ReportingController {
  private final CurrentPrincipal currentPrincipal;
  private final ReportingService reportingService;
  private final ReportingAccessAuditService reportingAccessAuditService;

  public ReportingController(
      CurrentPrincipal currentPrincipal,
      ReportingService reportingService,
      ReportingAccessAuditService reportingAccessAuditService
  ) {
    this.currentPrincipal = currentPrincipal;
    this.reportingService = reportingService;
    this.reportingAccessAuditService = reportingAccessAuditService;
  }

  @GetMapping("/api/admin/reports/definitions")
  public List<Map<String, Object>> indicatorDefinitions(Authentication authentication, HttpServletRequest request) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    reportingAccessAuditService.record(
        "ADMIN_REPORT_DEFINITIONS_READ",
        user.getUsername(),
        request.getRemoteAddr(),
        Map.of("path", request.getRequestURI())
    );
    return reportingService.listIndicatorDefinitions().stream()
        .map(d -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", d.getId());
          m.put("code", d.getCode());
          m.put("label", d.getLabel());
          m.put("description", d.getDescription());
          return m;
        })
        .toList();
  }

  @GetMapping("/api/admin/reports/inventory/{merchantId}")
  public Map<String, Object> inventorySummary(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("merchantId") String merchantId
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    reportingAccessAuditService.record(
        "ADMIN_REPORT_INVENTORY_SUMMARY_READ",
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

  @GetMapping("/api/admin/reports/inventory/{merchantId}/drill-down")
  public Map<String, Object> inventoryDrillDown(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("merchantId") String merchantId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    reportingAccessAuditService.record(
        "ADMIN_REPORT_INVENTORY_DRILLDOWN_READ",
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

  @GetMapping("/api/admin/reports/inventory/{merchantId}/export.csv")
  public ResponseEntity<byte[]> exportInventory(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("merchantId") String merchantId
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    reportingAccessAuditService.record(
        "ADMIN_REPORT_INVENTORY_EXPORT_READ",
        user.getUsername(),
        request.getRemoteAddr(),
        Map.of("merchantId", merchantId, "path", request.getRequestURI())
    );
    byte[] body = reportingService.exportInventoryCsv(merchantId);
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory_" + merchantId + ".csv\"")
        .body(body);
  }

  @GetMapping("/api/admin/reports/inventory/{merchantId}/daily")
  public List<Map<String, Object>> dailyHistory(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("merchantId") String merchantId,
      @RequestParam(defaultValue = "30") int limit
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    reportingAccessAuditService.record(
        "ADMIN_REPORT_DAILY_HISTORY_READ",
        user.getUsername(),
        request.getRemoteAddr(),
        Map.of("merchantId", merchantId, "limit", limit, "path", request.getRequestURI())
    );
    return reportingService.dailyHistory(merchantId, limit).stream()
        .map(r -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("reportDate", r.getReportDate());
          m.put("totalSkus", r.getTotalSkus());
          m.put("totalStock", r.getTotalStock());
          m.put("generatedAt", r.getGeneratedAt());
          return m;
        })
        .toList();
  }
}
