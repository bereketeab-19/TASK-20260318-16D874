package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;

import com.petsupplies.user.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class ContactEncryptionIntegrationTest extends AbstractIntegrationTest {

  @Autowired UserRepository userRepository;

  @AfterEach
  void clearBuyerContact() {
    var buyer = userRepository.findByUsername("buyer").orElseThrow();
    buyer.setContactEncrypted(null);
    userRepository.save(buyer);
  }

  @Test
  void contact_field_round_trips_through_aes_converter() {
    var buyer = userRepository.findByUsername("buyer").orElseThrow();
    buyer.setContactEncrypted("+15555550100");
    userRepository.saveAndFlush(buyer);

    assertThat(userRepository.findByUsername("buyer").orElseThrow().getContactEncrypted())
        .isEqualTo("+15555550100");
  }
}
