package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsupplies.messaging.repo.MessageRepository;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class MessagingImageHttpIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired MessageRepository messageRepository;

  @org.junit.jupiter.api.io.TempDir
  static Path tempDir;

  @DynamicPropertySource
  static void attachmentDir(DynamicPropertyRegistry registry) {
    registry.add("app.attachments.dir", () -> tempDir.toString());
  }

  @Test
  void post_image_message_persists_row() throws Exception {
    byte[] png = new byte[4096];
    byte[] sig = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(sig, 0, png, 0, sig.length);
    for (int i = sig.length; i < png.length; i++) {
      png[i] = (byte) (i % 251);
    }
    MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", png);

    String uploadJson = mockMvc.perform(multipart("/attachments").file(file).with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andReturn()
        .getResponse()
        .getContentAsString();
    long attachmentId = objectMapper.readTree(uploadJson).get("id").asLong();

    String sessionJson = mockMvc.perform(post("/sessions").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    long sessionId = objectMapper.readTree(sessionJson).get("id").asLong();

    mockMvc.perform(
            post("/sessions/{sessionId}/messages/image", sessionId)
                .with(httpBasic("merchantA", "merchantA123!"))
                .contentType(APPLICATION_JSON)
                .content("{\"attachmentId\":" + attachmentId + ",\"caption\":\"Photo\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messageId").exists());

    assertThat(messageRepository.count()).isGreaterThanOrEqualTo(1L);
  }
}
