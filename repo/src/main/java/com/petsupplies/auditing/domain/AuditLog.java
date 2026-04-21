package com.petsupplies.auditing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "action", nullable = false, length = 120)
  private String action;

  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  private String payload;

  @Column(name = "timestamp", nullable = false)
  private Instant timestamp;

  @Column(name = "actor_username", length = 80)
  private String actorUsername;

  @Column(name = "ip", length = 64)
  private String ip;

  protected AuditLog() {}

  public AuditLog(String action, String payload, Instant timestamp, String actorUsername, String ip) {
    this.action = action;
    this.payload = payload;
    this.timestamp = timestamp;
    this.actorUsername = actorUsername;
    this.ip = ip;
  }

  public Long getId() {
    return id;
  }

  public String getAction() {
    return action;
  }

  public String getPayload() {
    return payload;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getActorUsername() {
    return actorUsername;
  }

  public String getIp() {
    return ip;
  }
}

