package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petsupplies.product.repo.ProductRepository;
import com.petsupplies.product.repo.SkuRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class BatchImportAtomicityTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ProductRepository productRepository;
  @Autowired SkuRepository skuRepository;

  @Test
  void malformed_row_causes_zero_records_persisted() throws Exception {
    String csv = """
        productCode,productName,barcode,stockQuantity
        P1,Prod1,BAR1,5
        P2,Prod2,BAR2,5
        P3,Prod3,BAR3,5
        P4,Prod4,BAR4,5
        P5,Prod5,,5
        P6,Prod6,BAR6,5
        P7,Prod7,BAR7,5
        P8,Prod8,BAR8,5
        P9,Prod9,BAR9,5
        P10,Prod10,BAR10,5
        """;

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "skus.csv",
        "text/csv",
        csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/batch/import/skus/csv")
            .file(file)
            .with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isBadRequest());

    assertThat(productRepository.countByMerchantId("mrc_A")).isZero();
    assertThat(skuRepository.countByMerchantId("mrc_A")).isZero();
  }
}

