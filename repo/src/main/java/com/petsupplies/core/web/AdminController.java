package com.petsupplies.core.web;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/admin/ping")
  public Map<String, Object> ping() {
    return Map.of("status", "OK");
  }
}

