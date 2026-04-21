package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;

import com.petsupplies.achievement.repo.AchievementAttachmentVersionRepository;
import com.petsupplies.achievement.service.AchievementService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class AchievementAttachmentVersioningTest extends AbstractIntegrationTest {

  @TempDir
  static Path tempDir;

  @DynamicPropertySource
  static void registerAchievementAttachmentDir(DynamicPropertyRegistry registry) {
    registry.add("app.achievementAttachments.dir", () -> tempDir.toString());
  }

  @Autowired AchievementService achievementService;
  @Autowired AchievementAttachmentVersionRepository versionRepository;

  @Test
  void new_upload_creates_v2_keeps_v1_intact() throws Exception {
    var a = achievementService.create("mrc_A", 1L, "T", "P", "RP", "C", "merchantA");

    byte[] v1bytes = "v1".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] v2bytes = "v2".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    var v1 = achievementService.addAttachmentVersion("mrc_A", a.getId(),
        new MockMultipartFile("file", "a.txt", "text/plain", v1bytes), "merchantA");
    var v2 = achievementService.addAttachmentVersion("mrc_A", a.getId(),
        new MockMultipartFile("file", "b.txt", "text/plain", v2bytes), "merchantA");

    assertThat(v1.getVersion()).isEqualTo(1);
    assertThat(v2.getVersion()).isEqualTo(2);
    assertThat(versionRepository.findByAchievement_IdAndVersion(a.getId(), 1)).isPresent();
    assertThat(versionRepository.findByAchievement_IdAndVersion(a.getId(), 2)).isPresent();

    assertThat(Files.exists(Path.of(v1.getStoragePath()))).isTrue();
    assertThat(Files.exists(Path.of(v2.getStoragePath()))).isTrue();
    assertThat(Files.readAllBytes(Path.of(v1.getStoragePath()))).isEqualTo(v1bytes);
    assertThat(Files.readAllBytes(Path.of(v2.getStoragePath()))).isEqualTo(v2bytes);
  }
}

