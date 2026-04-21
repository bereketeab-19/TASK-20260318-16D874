package com.petsupplies;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ProductHeistIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void merchant_cannot_read_other_merchants_sku_returns_404() throws Exception {
    // MerchantA creates product + sku
    var productResp = mockMvc.perform(post("/products")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(new CreateProduct("P-A-1", "Prod A1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andReturn();

    long productId = objectMapper.readTree(productResp.getResponse().getContentAsString()).get("id").asLong();

    var skuResp = mockMvc.perform(post("/skus")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(new CreateSku(productId, "BAR-A-1", 5))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andReturn();

    long skuId = objectMapper.readTree(skuResp.getResponse().getContentAsString()).get("id").asLong();

    // MerchantB tries to read MerchantA's SKU -> must be 404 (no existence leakage)
    mockMvc.perform(get("/skus/" + skuId).with(httpBasic("merchantB", "merchantB123!")))
        .andExpect(status().isNotFound());
  }

  private record CreateProduct(String productCode, String name) {}
  private record CreateSku(Long productId, String barcode, int stockQuantity) {}
}

