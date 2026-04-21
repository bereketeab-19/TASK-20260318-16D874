package com.petsupplies;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SkuUpdateValidationIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void patch_sku_rejects_negative_stockQuantity() throws Exception {
    var productResp = mockMvc.perform(post("/products")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content("{\"productCode\":\"P-NEG-1\",\"name\":\"Neg test\"}"))
        .andExpect(status().isOk())
        .andReturn();
    long productId = objectMapper.readTree(productResp.getResponse().getContentAsString()).get("id").asLong();

    var skuResp = mockMvc.perform(post("/skus")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content("{\"productId\":" + productId + ",\"barcode\":\"BAR-NEG-1\",\"stockQuantity\":5}"))
        .andExpect(status().isOk())
        .andReturn();
    long skuId = objectMapper.readTree(skuResp.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(
            patch("/skus/" + skuId)
                .with(httpBasic("merchantA", "merchantA123!"))
                .contentType(APPLICATION_JSON)
                .content("{\"stockQuantity\":-1}"))
        .andExpect(status().isBadRequest());
  }
}
