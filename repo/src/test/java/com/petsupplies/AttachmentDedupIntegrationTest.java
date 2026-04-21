package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;

import com.petsupplies.messaging.repo.AttachmentRepository;
import com.petsupplies.messaging.repo.MessageRepository;
import com.petsupplies.messaging.service.AttachmentService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class AttachmentDedupIntegrationTest extends AbstractIntegrationTest {

  /** PNG magic bytes + filler (AttachmentService validates signatures). */
  static byte[] pngBytes(int len) {
    byte[] b = new byte[len];
    byte[] sig = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(sig, 0, b, 0, sig.length);
    for (int i = sig.length; i < len; i++) {
      b[i] = (byte) (i % 251);
    }
    return b;
  }

  @TempDir
  static Path tempDir;

  @DynamicPropertySource
  static void registerAttachmentDir(DynamicPropertyRegistry registry) {
    registry.add("app.attachments.dir", () -> tempDir.toString());
  }

  @Autowired AttachmentService attachmentService;
  @Autowired AttachmentRepository attachmentRepository;
  @Autowired MessageRepository messageRepository;

  @BeforeEach
  void cleanAttachments() throws IOException {
    messageRepository.deleteAll();
    attachmentRepository.deleteAll();
    if (Files.isDirectory(tempDir)) {
      try (var stream = Files.list(tempDir)) {
        stream.filter(Files::isRegularFile).forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (IOException ignored) {
            // best-effort cleanup between tests sharing one temp directory
          }
        });
      }
    }
  }

  @Test
  void storing_same_image_twice_creates_one_db_row_and_one_file() throws Exception {
    byte[] bytes = pngBytes(1024 * 1024);

    MockMultipartFile file1 = new MockMultipartFile("file", "a.png", "image/png", bytes);
    MockMultipartFile file2 = new MockMultipartFile("file", "b.png", "image/png", bytes);

    var a1 = attachmentService.store("mrc_A", file1);
    var a2 = attachmentService.store("mrc_A", file2);

    assertThat(a1.getId()).isEqualTo(a2.getId());
    assertThat(attachmentRepository.count()).isEqualTo(1);

    long fileCount = Files.list(tempDir).filter(Files::isRegularFile).count();
    assertThat(fileCount).isEqualTo(1);
  }

  @Test
  void same_bytes_different_merchants_create_separate_attachment_rows() throws Exception {
    byte[] bytes = pngBytes(2048);

    MockMultipartFile fileA = new MockMultipartFile("file", "a.png", "image/png", bytes);
    MockMultipartFile fileB = new MockMultipartFile("file", "b.png", "image/png", bytes);

    var idA = attachmentService.store("mrc_A", fileA).getId();
    var idB = attachmentService.store("mrc_B", fileB).getId();

    assertThat(idA).isNotEqualTo(idB);
    assertThat(attachmentRepository.count()).isGreaterThanOrEqualTo(2);
  }
}

