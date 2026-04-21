package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers message recall (sender-only + cross-tenant) and notification mark-read tenant isolation.
 */
@AutoConfigureMockMvc
@Sql(scripts = "/sql/merchant_a_coworker.sql")
class MessagingRecallAndNotificationAuthzIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @org.junit.jupiter.api.io.TempDir
  static Path tempDir;

  @DynamicPropertySource
  static void attachmentDir(DynamicPropertyRegistry registry) {
    registry.add("app.attachments.dir", () -> tempDir.toString());
  }

  @Test
  void recall_non_sender_same_tenant_forbidden_cross_tenant_not_found() throws Exception {
    byte[] png = new byte[4096];
    byte[] sig = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(sig, 0, png, 0, sig.length);
    for (int i = sig.length; i < png.length; i++) {
      png[i] = (byte) (i % 251);
    }
    MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", png);

    String uploadJson =
        mockMvc.perform(multipart("/attachments").file(file).with(httpBasic("merchantA", "merchantA123!")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long attachmentId = objectMapper.readTree(uploadJson).get("id").asLong();

    String sessionJson =
        mockMvc.perform(post("/sessions").with(httpBasic("merchantA", "merchantA123!")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long sessionId = objectMapper.readTree(sessionJson).get("id").asLong();

    String msgJson =
        mockMvc.perform(
                post("/sessions/{sessionId}/messages/image", sessionId)
                    .with(httpBasic("merchantA", "merchantA123!"))
                    .contentType(APPLICATION_JSON)
                    .content("{\"attachmentId\":" + attachmentId + "}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long messageId = objectMapper.readTree(msgJson).get("messageId").asLong();

    mockMvc.perform(
            post("/sessions/{sessionId}/messages/{messageId}/recall", sessionId, messageId)
                .with(httpBasic("merchantA2", "merchantA123!")))
        .andExpect(status().isForbidden());

    mockMvc.perform(
            post("/sessions/{sessionId}/messages/{messageId}/recall", sessionId, messageId)
                .with(httpBasic("merchantB", "merchantB123!")))
        .andExpect(status().isNotFound());
  }

  @Test
  void notification_mark_read_other_merchant_not_found() throws Exception {
    mockMvc.perform(
            post("/api/merchant/events/order-status")
                .with(httpBasic("merchantA", "merchantA123!"))
                .contentType(APPLICATION_JSON)
                .content("{\"orderRef\":\"ORD-X\",\"status\":\"SHIPPED\"}"))
        .andExpect(status().isOk());

    String listJson =
        mockMvc.perform(get("/notifications").with(httpBasic("merchantA", "merchantA123!")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode arr = objectMapper.readTree(listJson);
    assertThat(arr.isArray()).isTrue();
    assertThat(arr.size()).isGreaterThan(0);
    long notificationId = arr.get(0).get("id").asLong();

    mockMvc.perform(
            patch("/notifications/{id}/read", notificationId).with(httpBasic("merchantB", "merchantB123!")))
        .andExpect(status().isNotFound());
  }
}
