package com.petsupplies.messaging.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class AntiSpamCache {
  private final Cache<String, Boolean> recent;

  public AntiSpamCache() {
    this.recent = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(10))
        .maximumSize(50_000)
        .build();
  }

  /**
   * @return true if this key was newly seen, false if it was a duplicate within the TTL window.
   */
  public boolean markIfNew(String key) {
    Boolean existing = recent.asMap().putIfAbsent(key, Boolean.TRUE);
    return existing == null;
  }
}

