package com.petsupplies.reporting.service;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.notification.service.BusinessNotificationService;
import com.petsupplies.product.domain.Sku;
import com.petsupplies.product.repo.SkuRepository;
import com.petsupplies.reporting.domain.DailyInventoryReport;
import com.petsupplies.reporting.domain.ReportIndicatorDefinition;
import com.petsupplies.reporting.repo.DailyInventoryReportRepository;
import com.petsupplies.reporting.repo.ReportIndicatorDefinitionRepository;
import com.petsupplies.reporting.repo.ReportingRepository;
import com.petsupplies.user.repo.UserRepository;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {
  private final ReportingRepository reportingRepository;
  private final DailyInventoryReportRepository dailyInventoryReportRepository;
  private final ReportIndicatorDefinitionRepository reportIndicatorDefinitionRepository;
  private final SkuRepository skuRepository;
  private final UserRepository userRepository;
  private final Clock clock;
  private final AuditService auditService;
  private final BusinessNotificationService businessNotificationService;

  public ReportingService(
      ReportingRepository reportingRepository,
      DailyInventoryReportRepository dailyInventoryReportRepository,
      ReportIndicatorDefinitionRepository reportIndicatorDefinitionRepository,
      SkuRepository skuRepository,
      UserRepository userRepository,
      Clock clock,
      AuditService auditService,
      BusinessNotificationService businessNotificationService
  ) {
    this.reportingRepository = reportingRepository;
    this.dailyInventoryReportRepository = dailyInventoryReportRepository;
    this.reportIndicatorDefinitionRepository = reportIndicatorDefinitionRepository;
    this.skuRepository = skuRepository;
    this.userRepository = userRepository;
    this.clock = clock;
    this.auditService = auditService;
    this.businessNotificationService = businessNotificationService;
  }

  @Transactional(readOnly = true)
  public ReportingRepository.InventorySummaryRow inventorySummary(String merchantId) {
    return reportingRepository.inventorySummary(merchantId);
  }

  @Transactional(readOnly = true)
  public List<ReportIndicatorDefinition> listIndicatorDefinitions() {
    return reportIndicatorDefinitionRepository.findAllByOrderByCodeAsc();
  }

  @Transactional(readOnly = true)
  public Page<Map<String, Object>> inventoryDrillDown(String merchantId, int page, int size) {
    Pageable p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
    return skuRepository.findByMerchantIdAndActiveTrueOrderByIdDesc(merchantId, p)
        .map(s -> {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("skuId", s.getId());
          row.put("barcode", s.getBarcode());
          row.put("stockQuantity", s.getStockQuantity());
          row.put("productId", s.getProduct().getId());
          row.put("productCode", s.getProduct().getProductCode());
          return row;
        });
  }

  @Transactional(readOnly = true)
  public byte[] exportInventoryCsv(String merchantId) {
    List<Sku> rows = skuRepository.findByMerchantIdAndActiveTrueOrderByIdDesc(merchantId);
    StringWriter w = new StringWriter();
    w.write("sku_id,barcode,stock_quantity,product_id,product_code\n");
    for (Sku s : rows) {
      w.write(String.valueOf(s.getId()));
      w.write(',');
      w.write(escapeCsv(s.getBarcode()));
      w.write(',');
      w.write(String.valueOf(s.getStockQuantity()));
      w.write(',');
      w.write(String.valueOf(s.getProduct().getId()));
      w.write(',');
      w.write(escapeCsv(s.getProduct().getProductCode()));
      w.write('\n');
    }
    return w.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String escapeCsv(String v) {
    if (v == null) {
      return "";
    }
    String x = v.replace("\"", "\"\"");
    if (x.indexOf(',') >= 0 || x.indexOf('\n') >= 0 || x.indexOf('"') >= 0) {
      return "\"" + x + "\"";
    }
    return x;
  }

  @Transactional(readOnly = true)
  public List<DailyInventoryReport> dailyHistory(String merchantId, int limit) {
    return dailyInventoryReportRepository.findByMerchantIdOrderByReportDateDesc(
        merchantId,
        PageRequest.of(0, Math.min(365, Math.max(1, limit)))
    );
  }

  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void generateDailyInventoryReports() {
    LocalDate today = LocalDate.ofInstant(Instant.now(clock), ZoneOffset.UTC);
    for (String merchantId : userRepository.findDistinctMerchantIds()) {
      var row = reportingRepository.inventorySummary(merchantId);
      DailyInventoryReport r = dailyInventoryReportRepository
          .findByMerchantIdAndReportDate(merchantId, today)
          .orElseGet(DailyInventoryReport::new);

      r.setMerchantId(merchantId);
      r.setReportDate(today);
      r.setTotalSkus(row.getTotalSkus());
      r.setTotalStock(row.getTotalStock());
      r.setGeneratedAt(Instant.now(clock));
      dailyInventoryReportRepository.save(r);

      auditService.record(
          "REPORT_DAILY_INVENTORY_GENERATED",
          Map.of("merchantId", merchantId, "reportDate", today.toString(), "totalSkus", row.getTotalSkus(), "totalStock", row.getTotalStock()),
          "system",
          null
      );

      businessNotificationService.publishReportHandling(
          merchantId,
          "Daily inventory report ready for " + today + " (totalSkus=" + row.getTotalSkus() + ", totalStock=" + row.getTotalStock() + ")"
      );
    }
  }
}
