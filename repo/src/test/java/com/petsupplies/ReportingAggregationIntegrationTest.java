package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;

import com.petsupplies.product.service.InventoryService;
import com.petsupplies.product.service.ProductService;
import com.petsupplies.reporting.service.ReportingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportingAggregationIntegrationTest extends AbstractIntegrationTest {

  @Autowired ProductService productService;
  @Autowired InventoryService inventoryService;
  @Autowired ReportingService reportingService;

  @Test
  void inventory_summary_matches_skus_for_merchant() {
    // Dedicated merchant scope: shared DB retains SKUs from other tests on mrc_A (e.g. ProductHeist).
    String merchantId = "mrc_RPT_AGG";
    var p = productService.create(merchantId, "RPT-P-AGG-1", "ReportProd");
    inventoryService.createSku(merchantId, p, "RPT-BAR-AGG-1", 7, "merchantA", null);
    inventoryService.createSku(merchantId, p, "RPT-BAR-AGG-2", 3, "merchantA", null);

    var row = reportingService.inventorySummary(merchantId);
    assertThat(row.getTotalSkus()).isEqualTo(2);
    assertThat(row.getTotalStock()).isEqualTo(10);
  }
}

