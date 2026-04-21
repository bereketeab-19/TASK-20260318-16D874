package com.petsupplies.core.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PasswordPolicyTest {

  @Test
  void accepts_valid_password() {
    PasswordPolicy.validate("abc12345");
  }

  @Test
  void rejects_short_password() {
    assertThatThrownBy(() -> PasswordPolicy.validate("a1"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void rejects_missing_digit() {
    assertThatThrownBy(() -> PasswordPolicy.validate("abcdefgh"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void rejects_missing_letter() {
    assertThatThrownBy(() -> PasswordPolicy.validate("12345678"))
        .isInstanceOf(ResponseStatusException.class);
  }
}
