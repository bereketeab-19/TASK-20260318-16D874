package com.petsupplies.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sku_attribute_values")
public class SkuAttributeValue {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sku_id", nullable = false)
  private Sku sku;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attribute_definition_id", nullable = false)
  private AttributeDefinition attributeDefinition;

  @Column(name = "value_text", nullable = false, length = 512)
  private String valueText;

  public Long getId() {
    return id;
  }

  public Sku getSku() {
    return sku;
  }

  public void setSku(Sku sku) {
    this.sku = sku;
  }

  public AttributeDefinition getAttributeDefinition() {
    return attributeDefinition;
  }

  public void setAttributeDefinition(AttributeDefinition attributeDefinition) {
    this.attributeDefinition = attributeDefinition;
  }

  public String getValueText() {
    return valueText;
  }

  public void setValueText(String valueText) {
    this.valueText = valueText;
  }
}
