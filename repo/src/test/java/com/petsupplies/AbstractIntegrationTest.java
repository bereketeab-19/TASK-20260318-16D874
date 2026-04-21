package com.petsupplies;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Single shared MySQL container for all integration tests so Spring’s cached application context
 * keeps a valid JDBC URL and Hikari does not point at a stopped container.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName("petsupplies")
          .withUsername("root")
          .withPassword("rootpass")
          .withCommand("mysqld", "--log_bin_trust_function_creators=1")
          .withInitScript("testcontainers-init.sql");

  static {
    MYSQL.start();
  }

  @DynamicPropertySource
  static void registerSharedDatasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", () -> "app_user");
    registry.add("spring.datasource.password", () -> "app_pass");
    registry.add("spring.flyway.url", MYSQL::getJdbcUrl);
    registry.add("spring.flyway.user", () -> "migrator_user");
    registry.add("spring.flyway.password", () -> "migrator_pass");
    // Tests must not rely on a default production crypto key; use a fixed 32-byte hex key.
    registry.add(
        "app.crypto.hex-key",
        () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    );
  }
}
