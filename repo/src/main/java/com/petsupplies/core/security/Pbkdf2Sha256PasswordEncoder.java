package com.petsupplies.core.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Offline-friendly password hashing with PBKDF2-HMAC-SHA256.
 *
 * Stored format:
 * pbkdf2_sha256$<iterations>$<base64(salt)>$<base64(hash)>
 */
public class Pbkdf2Sha256PasswordEncoder implements PasswordEncoder {
  private static final String PREFIX = "pbkdf2_sha256";
  private static final int DEFAULT_ITERATIONS = 310_000;
  private static final int SALT_BYTES = 16;
  private static final int KEY_BITS = 256;

  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public String encode(CharSequence rawPassword) {
    byte[] salt = new byte[SALT_BYTES];
    secureRandom.nextBytes(salt);
    byte[] hash = pbkdf2(rawPassword.toString().toCharArray(), salt, DEFAULT_ITERATIONS, KEY_BITS);
    return format(DEFAULT_ITERATIONS, salt, hash);
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    Parsed parsed = parse(encodedPassword);
    byte[] candidate = pbkdf2(rawPassword.toString().toCharArray(), parsed.salt(), parsed.iterations(), parsed.keyBits());
    return constantTimeEquals(candidate, parsed.hash());
  }

  private static String format(int iterations, byte[] salt, byte[] hash) {
    return PREFIX
        + "$" + iterations
        + "$" + Base64.getEncoder().encodeToString(salt)
        + "$" + Base64.getEncoder().encodeToString(hash);
  }

  private static Parsed parse(String encoded) {
    if (encoded == null) throw new IllegalArgumentException("encoded password is null");
    String[] parts = encoded.split("\\$");
    if (parts.length != 4) throw new IllegalArgumentException("invalid encoded password format");
    if (!PREFIX.equals(parts[0])) throw new IllegalArgumentException("unsupported password format");

    int iterations = Integer.parseInt(parts[1]);
    byte[] salt = Base64.getDecoder().decode(parts[2]);
    byte[] hash = Base64.getDecoder().decode(parts[3]);
    return new Parsed(iterations, salt, hash, KEY_BITS);
  }

  private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
      SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      return skf.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("PBKDF2 not available", e);
    }
  }

  private static boolean constantTimeEquals(byte[] a, byte[] b) {
    if (a.length != b.length) return false;
    int result = 0;
    for (int i = 0; i < a.length; i++) {
      result |= a[i] ^ b[i];
    }
    return result == 0;
  }

  private record Parsed(int iterations, byte[] salt, byte[] hash, int keyBits) {}
}

