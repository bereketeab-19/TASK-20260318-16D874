package com.petsupplies.cooking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cooking_steps")
public class CookingStep {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "process_id", nullable = false)
  private CookingProcess process;

  @Column(name = "step_index", nullable = false)
  private int stepIndex;

  @Column(name = "instruction", nullable = false, columnDefinition = "TEXT")
  private String instruction;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "reminder_fire_at")
  private Instant reminderFireAt;

  public Long getId() {
    return id;
  }

  public CookingProcess getProcess() {
    return process;
  }

  public void setProcess(CookingProcess process) {
    this.process = process;
  }

  public int getStepIndex() {
    return stepIndex;
  }

  public void setStepIndex(int stepIndex) {
    this.stepIndex = stepIndex;
  }

  public String getInstruction() {
    return instruction;
  }

  public void setInstruction(String instruction) {
    this.instruction = instruction;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  public Instant getReminderFireAt() {
    return reminderFireAt;
  }

  public void setReminderFireAt(Instant reminderFireAt) {
    this.reminderFireAt = reminderFireAt;
  }
}

