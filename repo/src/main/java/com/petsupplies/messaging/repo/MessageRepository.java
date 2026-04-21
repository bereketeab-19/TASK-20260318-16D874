package com.petsupplies.messaging.repo;

import com.petsupplies.messaging.domain.Message;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
  long countByMerchantIdAndSession_Id(String merchantId, Long sessionId);

  @EntityGraph(attributePaths = {"session", "attachment"})
  Page<Message> findByMerchantIdAndSession_IdOrderBySentAtAsc(String merchantId, Long sessionId, Pageable pageable);

  Optional<Message> findByIdAndMerchantIdAndSession_Id(Long id, String merchantId, Long sessionId);

  @Modifying
  @Query("DELETE FROM Message m WHERE m.sentAt < :cutoff")
  int deleteOlderThan(@Param("cutoff") Instant cutoff);
}

