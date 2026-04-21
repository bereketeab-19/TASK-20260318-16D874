package com.petsupplies;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AttachmentMagicBytesIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;

  @org.junit.jupiter.api.io.TempDir
  static Path tempDir;

  @DynamicPropertySource
  static void attachmentDir(DynamicPropertyRegistry registry) {
    registry.add("app.attachments.dir", () -> tempDir.toString());
  }

  @Test
  void valid_jpeg_magic_bytes_accepted() throws Exception {
    byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};
    MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", jpeg);
    mockMvc.perform(multipart("/attachments").file(file).with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk());
  }

  @Test
  void valid_png_magic_bytes_accepted() throws Exception {
    byte[] png = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01
    };
    MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", png);
    mockMvc.perform(multipart("/attachments").file(file).with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk());
  }

  @Test
  void jpeg_content_type_with_png_bytes_rejected() throws Exception {
    byte[] pngSig = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    MockMultipartFile file = new MockMultipartFile("file", "spoof.jpg", "image/jpeg", pngSig);
    mockMvc.perform(multipart("/attachments").file(file).with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void garbage_bytes_rejected_for_png() throws Exception {
    byte[] garbage = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    MockMultipartFile file = new MockMultipartFile("file", "bad.png", "image/png", garbage);
    mockMvc.perform(multipart("/attachments").file(file).with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isBadRequest());
  }
}
