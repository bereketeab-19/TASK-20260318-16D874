package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsupplies.product.repo.InventoryLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class InventoryLogIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired InventoryLogRepository inventoryLogRepository;

  private record CreateProduct(String productCode, String name) {}

  private record CreateSku(Long productId, String barcode, int stockQuantity) {}

  private record PatchSku(Integer stockQuantity) {}

  @Test
  void stock_changes_emit_inventory_logs_and_list_endpoint_works() throws Exception {
    long before = inventoryLogRepository.count();

    var productResp = mockMvc.perform(post("/products")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateProduct("INV-LOG-P1", "Log prod"))))
        .andExpect(status().isOk())
        .andReturn();
    long productId = objectMapper.readTree(productResp.getResponse().getContentAsString()).get("id").asLong();

    var skuResp = mockMvc.perform(post("/skus")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateSku(productId, "INV-LOG-BAR-1", 4))))
        .andExpect(status().isOk())
        .andReturn();
    long skuId = objectMapper.readTree(skuResp.getResponse().getContentAsString()).get("id").asLong();

    assertThat(inventoryLogRepository.count()).isGreaterThan(before);

    mockMvc.perform(patch("/skus/" + skuId)
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PatchSku(10))))
        .andExpect(status().isOk());

    mockMvc.perform(get("/inventory/logs").param("skuId", String.valueOf(skuId)).with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").exists());
  }
}
