package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class MessagingMessagesQueryIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @org.junit.jupiter.api.io.TempDir
  static Path tempDir;

  @DynamicPropertySource
  static void attachmentDir(DynamicPropertyRegistry registry) {
    registry.add("app.attachments.dir", () -> tempDir.toString());
  }

  @Test
  void list_and_get_messages_include_read_state() throws Exception {
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

    String listJson =
        mockMvc.perform(get("/sessions/{sessionId}/messages", sessionId).with(httpBasic("merchantA", "merchantA123!")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].readAt").value(nullValue()))
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode list = objectMapper.readTree(listJson);
    assertThat(list.get("content").get(0).get("recalledAt").isNull()).isTrue();

    mockMvc.perform(
            post("/sessions/{sessionId}/messages/{messageId}/read", sessionId, messageId)
                .with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk());

    String oneJson =
        mockMvc.perform(
                get("/sessions/{sessionId}/messages/{messageId}", sessionId, messageId)
                    .with(httpBasic("merchantA", "merchantA123!")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.readAt").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(objectMapper.readTree(oneJson).get("readAt").isNull()).isFalse();
  }
}
