package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsupplies.notification.service.BusinessNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class BusinessEventNotificationIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void order_status_and_review_events_surface_in_notification_feed() throws Exception {
    mockMvc.perform(
            post("/api/merchant/events/order-status")
                .with(httpBasic("merchantA", "merchantA123!"))
                .contentType(APPLICATION_JSON)
                .content("{\"orderRef\":\"ORD-42\",\"status\":\"SHIPPED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventType").value(BusinessNotificationService.EVENT_ORDER_STATUS));

    mockMvc.perform(
            post("/api/merchant/events/review-outcome")
                .with(httpBasic("merchantA", "merchantA123!"))
                .contentType(APPLICATION_JSON)
                .content("{\"reviewRef\":\"REV-7\",\"outcome\":\"APPROVED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventType").value(BusinessNotificationService.EVENT_REVIEW_OUTCOME));

    MvcResult list = mockMvc.perform(get("/notifications").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode arr = objectMapper.readTree(list.getResponse().getContentAsString());
    assertThat(arr.isArray()).isTrue();
    assertThat(arr.toString()).contains("ORDER_STATUS");
    assertThat(arr.toString()).contains("REVIEW_OUTCOME");
  }
}
