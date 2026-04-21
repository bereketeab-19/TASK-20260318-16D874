package com.petsupplies.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_indicator_definitions")
public class ReportIndicatorDefinition {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, unique = true, length = 64)
  private String code;

  @Column(name = "label", nullable = false, length = 200)
  private String label;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }
}
