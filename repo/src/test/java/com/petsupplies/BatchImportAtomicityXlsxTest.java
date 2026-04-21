package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petsupplies.product.repo.ProductRepository;
import com.petsupplies.product.repo.SkuRepository;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class BatchImportAtomicityXlsxTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ProductRepository productRepository;
  @Autowired SkuRepository skuRepository;

  @Test
  void malformed_row_causes_zero_records_persisted_xlsx() throws Exception {
    byte[] xlsx = buildWorkbookBytes();

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "skus.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        xlsx
    );

    mockMvc.perform(multipart("/batch/import/skus/xlsx")
            .file(file)
            .with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isBadRequest());

    assertThat(productRepository.countByMerchantId("mrc_A")).isZero();
    assertThat(skuRepository.countByMerchantId("mrc_A")).isZero();
  }

  private static byte[] buildWorkbookBytes() throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      var sheet = wb.createSheet("import");
      var header = sheet.createRow(0);
      header.createCell(0).setCellValue("productCode");
      header.createCell(1).setCellValue("productName");
      header.createCell(2).setCellValue("barcode");
      header.createCell(3).setCellValue("stockQuantity");

      for (int i = 1; i <= 10; i++) {
        var r = sheet.createRow(i);
        r.createCell(0).setCellValue("P" + i);
        r.createCell(1).setCellValue("Prod" + i);
        if (i != 5) {
          r.createCell(2).setCellValue("BAR" + i);
        } else {
          r.createCell(2).setCellValue("");
        }
        r.createCell(3).setCellValue(5);
      }

      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        wb.write(out);
        return out.toByteArray();
      }
    }
  }
}

