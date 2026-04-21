package com.petsupplies.core.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AesGcmEncryptionService {
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  private final SecretKey secretKey;

  public AesGcmEncryptionService(@Value("${app.crypto.hex-key:}") String hexKey) {
    if (hexKey == null || hexKey.isBlank()) {
      throw new IllegalStateException(
          "Set APP_CRYPTO_HEX_KEY (or app.crypto.hex-key) to a 64-character hex string (32-byte AES-256 key). "
              + "Do not run production without a unique key."
      );
    }
    byte[] raw = HexFormat.of().parseHex(hexKey.trim());
    if (raw.length != 32) {
      throw new IllegalStateException("app.crypto.hex-key must decode to 32 bytes (AES-256)");
    }
    this.secretKey = new SecretKeySpec(raw, "AES");
  }

  public String encrypt(String plainText) {
    if (plainText == null) {
      return null;
    }
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
      buf.put(iv);
      buf.put(cipherText);
      return HexFormat.of().formatHex(buf.array());
    } catch (Exception e) {
      throw new IllegalStateException("Encryption failed", e);
    }
  }

  public String decrypt(String storedHex) {
    if (storedHex == null || storedHex.isBlank()) {
      return null;
    }
    try {
      byte[] all = HexFormat.of().parseHex(storedHex.trim());
      ByteBuffer buf = ByteBuffer.wrap(all);
      byte[] iv = new byte[GCM_IV_LENGTH];
      buf.get(iv);
      byte[] cipherBytes = new byte[buf.remaining()];
      buf.get(cipherBytes);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      byte[] plain = cipher.doFinal(cipherBytes);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Decryption failed", e);
    }
  }
}
