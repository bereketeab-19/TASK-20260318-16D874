package com.petsupplies;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class BuyerCatalogIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;

  @Test
  void unauthenticated_is_401() throws Exception {
    mockMvc.perform(get("/api/buyer/catalog/products"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void buyer_can_read_catalog() throws Exception {
    mockMvc.perform(get("/api/buyer/catalog/products").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());

    mockMvc.perform(get("/api/buyer/catalog/summary").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activeProductCount").exists())
        .andExpect(jsonPath("$.merchantCountWithActiveProducts").exists());
  }

  @Test
  void merchant_is_403() throws Exception {
    mockMvc.perform(get("/api/buyer/catalog/products").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void admin_is_403() throws Exception {
    mockMvc.perform(get("/api/buyer/catalog/summary").with(httpBasic("admin", "admin123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void reviewer_is_403() throws Exception {
    mockMvc.perform(get("/api/buyer/catalog/products").with(httpBasic("reviewer", "buyer123!")))
        .andExpect(status().isForbidden());
  }
}
