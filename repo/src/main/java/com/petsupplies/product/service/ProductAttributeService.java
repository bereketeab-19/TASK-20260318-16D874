package com.petsupplies.product.service;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.product.domain.AttributeDefinition;
import com.petsupplies.product.domain.Sku;
import com.petsupplies.product.domain.SkuAttributeValue;
import com.petsupplies.product.repo.AttributeDefinitionRepository;
import com.petsupplies.product.repo.SkuAttributeValueRepository;
import com.petsupplies.product.repo.SkuRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductAttributeService {
  private final AttributeDefinitionRepository attributeDefinitionRepository;
  private final SkuAttributeValueRepository skuAttributeValueRepository;
  private final SkuRepository skuRepository;
  private final AuditService auditService;

  public ProductAttributeService(
      AttributeDefinitionRepository attributeDefinitionRepository,
      SkuAttributeValueRepository skuAttributeValueRepository,
      SkuRepository skuRepository,
      AuditService auditService
  ) {
    this.attributeDefinitionRepository = attributeDefinitionRepository;
    this.skuAttributeValueRepository = skuAttributeValueRepository;
    this.skuRepository = skuRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<AttributeDefinition> listDefinitions(String merchantId) {
    return attributeDefinitionRepository.findByMerchantIdOrderByCodeAsc(merchantId);
  }

  @Transactional
  public AttributeDefinition createDefinition(String merchantId, String code, String label, String actorUsername) {
    AttributeDefinition a = new AttributeDefinition();
    a.setMerchantId(merchantId);
    a.setCode(code.trim());
    a.setLabel(label.trim());
    AttributeDefinition saved = attributeDefinitionRepository.save(a);
    auditService.record(
        "ATTRIBUTE_DEFINITION_CREATED",
        Map.of("merchantId", merchantId, "definitionId", saved.getId(), "code", saved.getCode()),
        actorUsername,
        null
    );
    return saved;
  }

  @Transactional
  public void deleteDefinition(String merchantId, Long id, String actorUsername) {
    AttributeDefinition a = attributeDefinitionRepository.findByIdAndMerchantId(id, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    attributeDefinitionRepository.delete(a);
    auditService.record(
        "ATTRIBUTE_DEFINITION_DELETED",
        Map.of("merchantId", merchantId, "definitionId", id),
        actorUsername,
        null
    );
  }

  @Transactional
  public SkuAttributeValue upsertSkuAttribute(
      String merchantId,
      Long skuId,
      Long attributeDefinitionId,
      String value,
      String actorUsername
  ) {
    Sku sku = skuRepository.findByIdAndMerchantId(skuId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SKU not found"));
    AttributeDefinition def = attributeDefinitionRepository.findByIdAndMerchantId(attributeDefinitionId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attribute definition not found"));

    SkuAttributeValue row = skuAttributeValueRepository
        .findBySku_IdAndAttributeDefinition_Id(skuId, def.getId())
        .orElseGet(SkuAttributeValue::new);
    row.setSku(sku);
    row.setAttributeDefinition(def);
    row.setValueText(value);
    SkuAttributeValue saved = skuAttributeValueRepository.save(row);
    auditService.record(
        "SKU_ATTRIBUTE_UPSERTED",
        Map.of("merchantId", merchantId, "skuId", skuId, "attributeDefinitionId", attributeDefinitionId),
        actorUsername,
        null
    );
    return saved;
  }

  @Transactional(readOnly = true)
  public List<SkuAttributeValue> listSkuAttributes(String merchantId, Long skuId) {
    skuRepository.findByIdAndMerchantId(skuId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    return skuAttributeValueRepository.findAllWithDefinitionBySku_Id(skuId);
  }
}
