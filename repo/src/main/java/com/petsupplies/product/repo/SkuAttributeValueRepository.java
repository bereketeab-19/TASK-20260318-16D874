package com.petsupplies.product.repo;

import com.petsupplies.product.domain.SkuAttributeValue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkuAttributeValueRepository extends JpaRepository<SkuAttributeValue, Long> {
  List<SkuAttributeValue> findAllBySku_Id(Long skuId);

  @Query("SELECT v FROM SkuAttributeValue v JOIN FETCH v.attributeDefinition WHERE v.sku.id = :skuId")
  List<SkuAttributeValue> findAllWithDefinitionBySku_Id(@Param("skuId") Long skuId);

  Optional<SkuAttributeValue> findBySku_IdAndAttributeDefinition_Id(Long skuId, Long attributeDefinitionId);

  void deleteAllBySku_Id(Long skuId);
}
