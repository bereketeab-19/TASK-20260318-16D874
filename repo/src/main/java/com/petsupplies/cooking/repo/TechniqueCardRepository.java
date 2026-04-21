package com.petsupplies.cooking.repo;

import com.petsupplies.cooking.domain.TechniqueCard;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechniqueCardRepository extends JpaRepository<TechniqueCard, Long> {
  @EntityGraph(attributePaths = {"tags"})
  Optional<TechniqueCard> findByIdAndMerchantId(Long id, String merchantId);

  @EntityGraph(attributePaths = {"tags"})
  Page<TechniqueCard> findByMerchantIdOrderByUpdatedAtDesc(String merchantId, Pageable pageable);

  @EntityGraph(attributePaths = {"tags"})
  @Query(
      "select distinct c from TechniqueCard c join c.tags t "
          + "where c.merchantId = :merchantId and lower(t.name) = lower(:tagName)"
  )
  Page<TechniqueCard> findByMerchantIdAndTagName(
      @Param("merchantId") String merchantId,
      @Param("tagName") String tagName,
      Pageable pageable
  );
}
