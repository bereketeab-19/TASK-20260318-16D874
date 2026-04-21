package com.petsupplies.core.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PasswordPolicy {
  private PasswordPolicy() {}

  /**
   * Enforces: length >= 8, at least one letter, at least one digit (prompt-aligned baseline).
   */
  public static void validate(String rawPassword) {
    if (rawPassword == null || rawPassword.length() < 8) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
    }
    boolean hasLetter = rawPassword.chars().anyMatch(Character::isLetter);
    boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
    if (!hasLetter || !hasDigit) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain letters and digits");
    }
  }
}
