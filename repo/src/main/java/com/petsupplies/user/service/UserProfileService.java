package com.petsupplies.user.service;

import com.petsupplies.core.validation.PasswordPolicy;
import com.petsupplies.user.domain.User;
import com.petsupplies.user.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public void changePassword(Long userId, String newPlainPassword) {
    PasswordPolicy.validate(newPlainPassword);
    User u = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    u.setPasswordHash(passwordEncoder.encode(newPlainPassword));
  }

  @Transactional
  public void updateContactPhone(Long userId, String plainContact) {
    User u = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    if (plainContact == null || plainContact.isBlank()) {
      u.setContactEncrypted(null);
    } else {
      u.setContactEncrypted(plainContact.trim());
    }
  }
}
