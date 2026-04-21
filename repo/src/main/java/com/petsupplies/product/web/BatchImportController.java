package com.petsupplies.product.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.product.service.BatchImportService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class BatchImportController {
  private final CurrentPrincipal currentPrincipal;
  private final BatchImportService batchImportService;

  public BatchImportController(CurrentPrincipal currentPrincipal, BatchImportService batchImportService) {
    this.currentPrincipal = currentPrincipal;
    this.batchImportService = batchImportService;
  }

  @PostMapping(value = "/batch/import/skus/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> importSkusCsv(
      Authentication authentication,
      HttpServletRequest request,
      @RequestPart("file") MultipartFile file
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var result = batchImportService.importSkusCsv(merchantId, file, user.getUsername(), request.getRemoteAddr());
    return Map.of(
        "rowsRead", result.rowsRead(),
        "productsCreated", result.productsCreated(),
        "skusCreated", result.skusCreated()
    );
  }

  @PostMapping(value = "/batch/import/skus/xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> importSkusXlsx(
      Authentication authentication,
      HttpServletRequest request,
      @RequestPart("file") MultipartFile file
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var result = batchImportService.importSkusXlsx(merchantId, file, user.getUsername(), request.getRemoteAddr());
    return Map.of(
        "rowsRead", result.rowsRead(),
        "productsCreated", result.productsCreated(),
        "skusCreated", result.skusCreated()
    );
  }

  @GetMapping(value = "/batch/export/skus/csv", produces = "text/csv")
  public String exportSkusCsv(Authentication authentication) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return batchImportService.exportSkusCsv(merchantId);
  }
}

