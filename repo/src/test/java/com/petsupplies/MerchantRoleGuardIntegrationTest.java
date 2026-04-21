package com.petsupplies;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Merchant REST controllers require {@code ROLE_MERCHANT}; buyers must not call merchant-business routes.
 */
@AutoConfigureMockMvc
class MerchantRoleGuardIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;

  @Test
  void buyer_cannot_list_products() throws Exception {
    mockMvc.perform(get("/products").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void buyer_cannot_create_session() throws Exception {
    mockMvc.perform(post("/sessions").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void buyer_cannot_list_notifications() throws Exception {
    mockMvc.perform(get("/notifications").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void buyer_cannot_list_inventory_logs() throws Exception {
    mockMvc.perform(get("/inventory/logs").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isForbidden());
  }
}
