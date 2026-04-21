package com.petsupplies.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.reporting.domain.CustomReportDefinition;
import com.petsupplies.reporting.repo.CustomReportDefinitionRepository;
import com.petsupplies.reporting.repo.ReportingRepository;
import com.petsupplies.product.repo.InventoryLogRepository;
import com.petsupplies.reporting.web.dto.CreateCustomReportRequest;
import com.petsupplies.reporting.web.dto.UpdateCustomReportRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomReportService {
  public static final String TPL_INVENTORY_SUMMARY = "INVENTORY_SUMMARY";
  public static final String TPL_INVENTORY_DRILL_DOWN = "INVENTORY_DRILL_DOWN";
  /** Time-series of inventory movement units from {@code inventory_logs} (half-open date range {@code from} .. {@code to}). */
  public static final String TPL_INVENTORY_MOVEMENT_TIMELINE = "INVENTORY_MOVEMENT_TIMELINE";

  private final CustomReportDefinitionRepository customReportDefinitionRepository;
  private final ReportingService reportingService;
  private final ReportingRepository reportingRepository;
  private final InventoryLogRepository inventoryLogRepository;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public CustomReportService(
      CustomReportDefinitionRepository customReportDefinitionRepository,
      ReportingService reportingService,
      ReportingRepository reportingRepository,
      InventoryLogRepository inventoryLogRepository,
      AuditService auditService,
      ObjectMapper objectMapper,
      Clock clock
  ) {
    this.customReportDefinitionRepository = customReportDefinitionRepository;
    this.reportingService = reportingService;
    this.reportingRepository = reportingRepository;
    this.inventoryLogRepository = inventoryLogRepository;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public CustomReportDefinition requireScoped(Long id, String merchantId) {
    return customReportDefinitionRepository.findByIdAndMerchantId(id, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listRows(String merchantId) {
    return customReportDefinitionRepository.findByMerchantIdOrderByNameAsc(merchantId).stream()
        .map(this::toRow)
        .collect(Collectors.toList());
  }

  @Transactional
  public Map<String, Object> create(String merchantId, Long ownerUserId, String actorUsername, CreateCustomReportRequest req) {
    validateDefinitionJson(req.definitionJson());
    Instant now = Instant.now(clock);
    CustomReportDefinition e = new CustomReportDefinition();
    e.setMerchantId(merchantId);
    e.setName(req.name().trim());
    e.setDescription(req.description() != null ? req.description().trim() : null);
    e.setDefinitionJson(req.definitionJson().trim());
    e.setScheduleCron(blankToNull(req.scheduleCron()));
    e.setScheduleTimezone(blankToNull(req.scheduleTimezone()));
    e.setOwnerUserId(ownerUserId);
    e.setActive(true);
    e.setCreatedAt(now);
    e.setUpdatedAt(now);
    CustomReportDefinition saved = customReportDefinitionRepository.save(e);
    auditService.record(
        "CUSTOM_REPORT_CREATED",
        Map.of("reportId", saved.getId(), "merchantId", merchantId, "name", saved.getName()),
        actorUsername,
        null
    );
    return toRow(saved);
  }

  @Transactional
  public Map<String, Object> update(
      String merchantId,
      Long id,
      String actorUsername,
      UpdateCustomReportRequest req
  ) {
    CustomReportDefinition e = requireScoped(id, merchantId);
    if (req.name() != null && !req.name().isBlank()) {
      e.setName(req.name().trim());
    }
    if (req.description() != null) {
      e.setDescription(req.description().isBlank() ? null : req.description().trim());
    }
    if (req.definitionJson() != null && !req.definitionJson().isBlank()) {
      validateDefinitionJson(req.definitionJson());
      e.setDefinitionJson(req.definitionJson().trim());
    }
    if (req.scheduleCron() != null) {
      e.setScheduleCron(blankToNull(req.scheduleCron()));
    }
    if (req.scheduleTimezone() != null) {
      e.setScheduleTimezone(blankToNull(req.scheduleTimezone()));
    }
    if (req.active() != null) {
      e.setActive(req.active());
    }
    e.setUpdatedAt(Instant.now(clock));
    customReportDefinitionRepository.save(e);
    auditService.record(
        "CUSTOM_REPORT_UPDATED",
        Map.of("reportId", id, "merchantId", merchantId, "name", e.getName()),
        actorUsername,
        null
    );
    return toRow(e);
  }

  @Transactional
  public void delete(String merchantId, Long id, String actorUsername) {
    CustomReportDefinition e = requireScoped(id, merchantId);
    customReportDefinitionRepository.delete(e);
    auditService.record(
        "CUSTOM_REPORT_DELETED",
        Map.of("reportId", id, "merchantId", merchantId, "name", e.getName()),
        actorUsername,
        null
    );
  }

  @Transactional(readOnly = true)
  public Map<String, Object> execute(String merchantId, Long id, String actorUsername, String ip) {
    CustomReportDefinition e = requireScoped(id, merchantId);
    if (!e.isActive()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report is inactive");
    }
    Map<String, Object> data = runDefinition(merchantId, e.getDefinitionJson());
    auditService.record(
        "CUSTOM_REPORT_EXECUTED",
        Map.of("reportId", id, "merchantId", merchantId, "name", e.getName()),
        actorUsername,
        ip
    );
    return Map.of(
        "reportId", id,
        "name", e.getName(),
        "data", data
    );
  }

  private Map<String, Object> runDefinition(String merchantId, String definitionJson) {
    JsonNode root = readJson(definitionJson);
    String template = root.path("template").asText(null);
    if (template == null || template.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "definitionJson must include template");
    }
    return switch (template) {
      case TPL_INVENTORY_SUMMARY -> inventorySummaryData(merchantId);
      case TPL_INVENTORY_DRILL_DOWN -> inventoryDrillDownData(merchantId, root);
      case TPL_INVENTORY_MOVEMENT_TIMELINE -> movementTimelineData(merchantId, root);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown template: " + template);
    };
  }

  private Map<String, Object> inventorySummaryData(String merchantId) {
    var row = reportingRepository.inventorySummary(merchantId);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("template", TPL_INVENTORY_SUMMARY);
    m.put("merchantId", merchantId);
    m.put("totalSkus", row.getTotalSkus());
    m.put("totalStock", row.getTotalStock());
    return m;
  }

  private Map<String, Object> inventoryDrillDownData(String merchantId, JsonNode root) {
    int page = root.path("page").asInt(0);
    int size = root.path("size").asInt(20);
    Page<Map<String, Object>> p = reportingService.inventoryDrillDown(merchantId, page, size);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("template", TPL_INVENTORY_DRILL_DOWN);
    m.put("merchantId", merchantId);
    m.put("page", p.getNumber());
    m.put("totalPages", p.getTotalPages());
    m.put("totalElements", p.getTotalElements());
    m.put("rows", p.getContent());
    return m;
  }

  private Map<String, Object> movementTimelineData(String merchantId, JsonNode root) {
    String from = root.path("from").asText(null);
    String to = root.path("to").asText(null);
    if (from == null || to == null || from.isBlank() || to.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "definitionJson must include from and to (yyyy-MM-dd) for INVENTORY_MOVEMENT_TIMELINE"
      );
    }
    LocalDate dFrom;
    LocalDate dTo;
    try {
      dFrom = LocalDate.parse(from);
      dTo = LocalDate.parse(to);
    } catch (DateTimeParseException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid from/to date (use yyyy-MM-dd)");
    }
    if (!dTo.isAfter(dFrom)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must be strictly after from");
    }
    Instant fromInc = dFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant toExc = dTo.atStartOfDay(ZoneOffset.UTC).toInstant();
    List<Object[]> raw = inventoryLogRepository.sumMovementByDay(merchantId, fromInc, toExc);
    List<Map<String, Object>> series = new ArrayList<>();
    for (Object[] row : raw) {
      Map<String, Object> point = new LinkedHashMap<>();
      point.put("day", row[0] == null ? null : row[0].toString());
      long units = row[1] instanceof Number n ? n.longValue() : 0L;
      long events = row[2] instanceof Number n ? n.longValue() : 0L;
      point.put("movementUnits", units);
      point.put("eventCount", events);
      series.add(point);
    }
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("template", TPL_INVENTORY_MOVEMENT_TIMELINE);
    m.put("merchantId", merchantId);
    m.put("from", from);
    m.put("to", to);
    m.put("timezone", "UTC");
    m.put("series", series);
    return m;
  }

  private void validateDefinitionJson(String json) {
    JsonNode root = readJson(json);
    if (!root.has("template") || root.path("template").asText("").isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "definitionJson must include template");
    }
  }

  private JsonNode readJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid definitionJson");
    }
  }

  private static String blankToNull(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    return s.trim();
  }

  public Map<String, Object> toRow(CustomReportDefinition e) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", e.getId());
    m.put("merchantId", e.getMerchantId());
    m.put("name", e.getName());
    m.put("description", e.getDescription());
    m.put("definitionJson", e.getDefinitionJson());
    m.put("scheduleCron", e.getScheduleCron());
    m.put("scheduleTimezone", e.getScheduleTimezone());
    m.put("ownerUserId", e.getOwnerUserId());
    m.put("active", e.isActive());
    m.put("createdAt", e.getCreatedAt());
    m.put("updatedAt", e.getUpdatedAt());
    return m;
  }
}
