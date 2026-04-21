package com.petsupplies.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreConfig {
  @Bean
  @ConditionalOnMissingBean(Clock.class)
  public Clock systemClock() {
    return Clock.systemUTC();
  }
}

