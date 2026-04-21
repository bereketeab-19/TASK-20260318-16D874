package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petsupplies.notification.repo.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class NotificationSubscriptionEnforcementIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired NotificationRepository notificationRepository;

  @Test
  void order_status_event_does_not_persist_notification_when_disabled() throws Exception {
    String merchantId = "mrc_A";
    long before = notificationRepository.countByMerchantId(merchantId);

    mockMvc.perform(
            put("/notifications/subscriptions")
                .with(httpBasic("merchantA", "merchantA123!"))
                .contentType(APPLICATION_JSON)
                .content("{\"eventType\":\"ORDER_STATUS\",\"enabled\":false}"))
        .andExpect(status().isOk());

    mockMvc.perform(
            post("/api/merchant/events/order-status")
                .with(httpBasic("merchantA", "merchantA123!"))
                .contentType(APPLICATION_JSON)
                .content("{\"orderRef\":\"ORD-SUB-1\",\"status\":\"SHIPPED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notificationPersisted").value(false));

    assertThat(notificationRepository.countByMerchantId(merchantId)).isEqualTo(before);

    mockMvc.perform(
            put("/notifications/subscriptions")
                .with(httpBasic("merchantA", "merchantA123!"))
                .contentType(APPLICATION_JSON)
                .content("{\"eventType\":\"ORDER_STATUS\",\"enabled\":true}"))
        .andExpect(status().isOk());
  }

  @Test
  void order_status_persists_when_subscription_re_enabled() throws Exception {
    mockMvc.perform(
            put("/notifications/subscriptions")
                .with(httpBasic("merchantA", "merchantA123!"))
                .contentType(APPLICATION_JSON)
                .content("{\"eventType\":\"ORDER_STATUS\",\"enabled\":true}"))
        .andExpect(status().isOk());

    mockMvc.perform(
            post("/api/merchant/events/order-status")
                .with(httpBasic("merchantA", "merchantA123!"))
                .contentType(APPLICATION_JSON)
                .content("{\"orderRef\":\"ORD-SUB-2\",\"status\":\"SHIPPED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notificationPersisted").value(true));

    mockMvc.perform(get("/notifications").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk());
  }
}
