package com.petsupplies.messaging.service;

import com.petsupplies.messaging.domain.Attachment;
import com.petsupplies.messaging.repo.AttachmentRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttachmentService {
  private static final long MAX_BYTES = 2L * 1024 * 1024;

  private final AttachmentRepository attachmentRepository;
  private final Clock clock;
  private final Path attachmentsDir;

  public AttachmentService(
      AttachmentRepository attachmentRepository,
      Clock clock,
      @Value("${app.attachments.dir}") String attachmentsDir
  ) {
    this.attachmentRepository = attachmentRepository;
    this.clock = clock;
    this.attachmentsDir = Path.of(attachmentsDir);
  }

  @Transactional
  public Attachment store(String merchantId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large");
    }
    String ct = file.getContentType();
    if (ct == null || !(ct.equals("image/jpeg") || ct.equals("image/png"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG/PNG allowed");
    }

    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read file");
    }

    validateImageSignature(ct, bytes);

    String sha256 = DigestUtils.sha256Hex(bytes);
    var existingSameMerchant = attachmentRepository.findByMerchantIdAndSha256(merchantId, sha256);
    if (existingSameMerchant.isPresent()) {
      return existingSameMerchant.get();
    }

    String ext = ct.equals("image/png") ? ".png" : ".jpg";
    String filename = sha256 + ext;
    Path target = attachmentsDir.resolve(filename).normalize();

    try {
      Files.createDirectories(attachmentsDir);
      if (!target.startsWith(attachmentsDir)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attachment path");
      }
      if (!Files.exists(target)) {
        Files.write(target, bytes);
      }
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
    }

    Attachment a = new Attachment();
    a.setMerchantId(merchantId);
    a.setSha256(sha256);
    a.setContentType(ct);
    a.setSizeBytes(bytes.length);
    a.setStoragePath(target.toString());
    a.setCreatedAt(Instant.now(clock));
    return attachmentRepository.save(a);
  }

  /**
   * Rejects uploads where {@code Content-Type} does not match actual JPEG/PNG file signatures
   * (magic bytes), even if the client claims {@code image/jpeg} or {@code image/png}.
   */
  private static void validateImageSignature(String contentType, byte[] bytes) {
    boolean jpeg = isJpegMagic(bytes);
    boolean png = isPngMagic(bytes);
    if ("image/jpeg".equals(contentType)) {
      if (!jpeg) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPEG signature mismatch");
      }
      return;
    }
    if ("image/png".equals(contentType)) {
      if (!png) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PNG signature mismatch");
      }
      return;
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG/PNG allowed");
  }

  private static boolean isJpegMagic(byte[] bytes) {
    return bytes.length >= 3
        && (bytes[0] & 0xFF) == 0xFF
        && (bytes[1] & 0xFF) == 0xD8
        && (bytes[2] & 0xFF) == 0xFF;
  }

  private static boolean isPngMagic(byte[] bytes) {
    if (bytes.length < 8) {
      return false;
    }
    return (bytes[0] & 0xFF) == 0x89
        && bytes[1] == 0x50
        && bytes[2] == 0x4E
        && bytes[3] == 0x47
        && bytes[4] == 0x0D
        && bytes[5] == 0x0A
        && bytes[6] == 0x1A
        && bytes[7] == 0x0A;
  }
}

