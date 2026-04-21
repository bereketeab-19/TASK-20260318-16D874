package com.petsupplies.product.service;

import com.opencsv.CSVReader;
import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.product.domain.Product;
import com.petsupplies.product.repo.ProductRepository;
import com.petsupplies.product.repo.SkuRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BatchImportService {
  private final ProductRepository productRepository;
  private final InventoryService inventoryService;
  private final SkuRepository skuRepository;
  private final AuditService auditService;
  private final Clock clock;

  public BatchImportService(
      ProductRepository productRepository,
      InventoryService inventoryService,
      SkuRepository skuRepository,
      AuditService auditService,
      Clock clock
  ) {
    this.productRepository = productRepository;
    this.inventoryService = inventoryService;
    this.skuRepository = skuRepository;
    this.auditService = auditService;
    this.clock = clock;
  }

  public record ImportResult(int rowsRead, int productsCreated, int skusCreated) {}

  /**
   * CSV schema (with header):
   * productCode,productName,barcode,stockQuantity
   */
  @Transactional
  public ImportResult importSkusCsv(
      String merchantId,
      MultipartFile file,
      String actorUsername,
      String ip
  ) {
    List<Row> rows = readAndValidateCsv(file);

    Map<String, Product> productsByCode = new HashMap<>();
    int productsCreated = 0;
    int skusCreated = 0;

    try {
      for (Row r : rows) {
        Product product = productsByCode.get(r.productCode);
        if (product == null) {
          product = productRepository.findByProductCodeAndMerchantId(r.productCode, merchantId).orElse(null);
          if (product == null) {
            Product p = new Product();
            p.setMerchantId(merchantId);
            p.setProductCode(r.productCode);
            p.setName(r.productName);
            p.setCreatedAt(Instant.now(clock));
            product = productRepository.save(p);
            productsCreated++;
          }
          productsByCode.put(r.productCode, product);
        }

        inventoryService.createSku(merchantId, product, r.barcode, r.stockQuantity, actorUsername, ip, "BATCH_IMPORT");
        skusCreated++;
      }
    } catch (DataIntegrityViolationException e) {
      // All-or-nothing: any constraint violation must roll back the transaction.
      auditService.record(
          "BATCH_IMPORT_SKUS_CSV_FAILED",
          Map.of("merchantId", merchantId, "rows", rows.size(), "error", "constraint_violation"),
          actorUsername,
          ip
      );
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch import failed (constraint violation)");
    } catch (BatchImportException e) {
      auditService.record(
          "BATCH_IMPORT_SKUS_CSV_FAILED",
          Map.of("merchantId", merchantId, "rows", rows.size(), "error", e.getMessage()),
          actorUsername,
          ip
      );
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    auditService.record(
        "BATCH_IMPORT_SKUS_CSV",
        Map.of("merchantId", merchantId, "rows", rows.size(), "skusCreated", skusCreated),
        actorUsername,
        ip
    );

    return new ImportResult(rows.size(), productsCreated, skusCreated);
  }

  /**
   * XLSX schema (Sheet 0, row 0 header):
   * productCode | productName | barcode | stockQuantity
   */
  @Transactional
  public ImportResult importSkusXlsx(
      String merchantId,
      MultipartFile file,
      String actorUsername,
      String ip
  ) {
    List<Row> rows = readAndValidateXlsx(file);

    Map<String, Product> productsByCode = new HashMap<>();
    int productsCreated = 0;
    int skusCreated = 0;

    try {
      for (Row r : rows) {
        Product product = productsByCode.get(r.productCode);
        if (product == null) {
          product = productRepository.findByProductCodeAndMerchantId(r.productCode, merchantId).orElse(null);
          if (product == null) {
            Product p = new Product();
            p.setMerchantId(merchantId);
            p.setProductCode(r.productCode);
            p.setName(r.productName);
            p.setCreatedAt(Instant.now(clock));
            product = productRepository.save(p);
            productsCreated++;
          }
          productsByCode.put(r.productCode, product);
        }

        inventoryService.createSku(merchantId, product, r.barcode, r.stockQuantity, actorUsername, ip, "BATCH_IMPORT");
        skusCreated++;
      }
    } catch (DataIntegrityViolationException e) {
      auditService.record(
          "BATCH_IMPORT_SKUS_XLSX_FAILED",
          Map.of("merchantId", merchantId, "rows", rows.size(), "error", "constraint_violation"),
          actorUsername,
          ip
      );
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch import failed (constraint violation)");
    } catch (BatchImportException e) {
      auditService.record(
          "BATCH_IMPORT_SKUS_XLSX_FAILED",
          Map.of("merchantId", merchantId, "rows", rows.size(), "error", e.getMessage()),
          actorUsername,
          ip
      );
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    auditService.record(
        "BATCH_IMPORT_SKUS_XLSX",
        Map.of("merchantId", merchantId, "rows", rows.size(), "skusCreated", skusCreated),
        actorUsername,
        ip
    );

    return new ImportResult(rows.size(), productsCreated, skusCreated);
  }

  @Transactional(readOnly = true)
  public String exportSkusCsv(String merchantId) {
    StringBuilder sb = new StringBuilder();
    sb.append("skuId,barcode,stockQuantity\n");
    for (var sku : skuRepository.findAllByMerchantId(merchantId)) {
      sb.append(sku.getId()).append(',')
          .append(escapeCsv(sku.getBarcode())).append(',')
          .append(sku.getStockQuantity())
          .append('\n');
    }
    return sb.toString();
  }

  private static String escapeCsv(String v) {
    if (v == null) return "";
    if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
      return "\"" + v.replace("\"", "\"\"") + "\"";
    }
    return v;
  }

  private List<Row> readAndValidateCsv(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BatchImportException("File is empty");
    }
    List<Row> rows = new ArrayList<>();
    Set<String> seenBarcodes = new HashSet<>();

    try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String[] header = reader.readNext();
      if (header == null || header.length < 4) {
        throw new BatchImportException("Missing header");
      }
      String[] line;
      int rowNum = 1;
      while ((line = reader.readNext()) != null) {
        rowNum++;
        if (line.length < 4) {
          throw new BatchImportException("Row " + rowNum + " has insufficient columns");
        }
        String productCode = req(line[0], "Row " + rowNum + " productCode is required");
        String productName = req(line[1], "Row " + rowNum + " productName is required");
        String barcode = req(line[2], "Row " + rowNum + " barcode is required");
        int qty = parseNonNegativeInt(line[3], "Row " + rowNum + " stockQuantity invalid");

        if (!seenBarcodes.add(barcode)) {
          throw new BatchImportException("Duplicate barcode in file at row " + rowNum);
        }
        rows.add(new Row(productCode, productName, barcode, qty));
      }
    } catch (BatchImportException e) {
      throw e;
    } catch (Exception e) {
      throw new BatchImportException("Failed to read CSV");
    }

    if (rows.isEmpty()) {
      throw new BatchImportException("No data rows");
    }
    return rows;
  }

  private List<Row> readAndValidateXlsx(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BatchImportException("File is empty");
    }
    List<Row> rows = new ArrayList<>();
    Set<String> seenBarcodes = new HashSet<>();

    try {
      byte[] bytes = file.getBytes();
      try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
        var sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
        if (sheet == null) throw new BatchImportException("Missing sheet");

        var header = sheet.getRow(0);
        if (header == null) throw new BatchImportException("Missing header row");

        int last = sheet.getLastRowNum();
        for (int r = 1; r <= last; r++) {
          var row = sheet.getRow(r);
          if (row == null) continue;

          String productCode = req(cellString(row, 0), "Row " + (r + 1) + " productCode is required");
          String productName = req(cellString(row, 1), "Row " + (r + 1) + " productName is required");
          String barcode = req(cellString(row, 2), "Row " + (r + 1) + " barcode is required");
          int qty = parseNonNegativeInt(cellString(row, 3), "Row " + (r + 1) + " stockQuantity invalid");

          if (!seenBarcodes.add(barcode)) {
            throw new BatchImportException("Duplicate barcode in file at row " + (r + 1));
          }
          rows.add(new Row(productCode, productName, barcode, qty));
        }
      }
    } catch (BatchImportException e) {
      throw e;
    } catch (Exception e) {
      throw new BatchImportException("Failed to read XLSX");
    }

    if (rows.isEmpty()) {
      throw new BatchImportException("No data rows");
    }
    return rows;
  }

  private static String cellString(org.apache.poi.ss.usermodel.Row row, int idx) {
    var cell = row.getCell(idx, MissingCellPolicy.RETURN_BLANK_AS_NULL);
    if (cell == null) return null;
    if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
    if (cell.getCellType() == CellType.NUMERIC) {
      double d = cell.getNumericCellValue();
      if (Math.floor(d) == d) return Long.toString((long) d);
      return Double.toString(d);
    }
    if (cell.getCellType() == CellType.BOOLEAN) return Boolean.toString(cell.getBooleanCellValue());
    if (cell.getCellType() == CellType.FORMULA) {
      return cell.getCellFormula();
    }
    return null;
  }

  private static String req(String v, String msg) {
    if (v == null) return fail(msg);
    String t = v.trim();
    if (t.isEmpty()) return fail(msg);
    return t;
  }

  private static int parseNonNegativeInt(String v, String msg) {
    try {
      int i = Integer.parseInt(v.trim());
      if (i < 0) throw new NumberFormatException("negative");
      return i;
    } catch (Exception e) {
      throw new BatchImportException(msg);
    }
  }

  private static String fail(String msg) {
    throw new BatchImportException(msg);
  }

  private record Row(String productCode, String productName, String barcode, int stockQuantity) {}
}

