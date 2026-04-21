package com.petsupplies.messaging.repo;

import com.petsupplies.messaging.domain.Session;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {
  Optional<Session> findByIdAndMerchantId(Long id, String merchantId);

  List<Session> findTop50ByMerchantIdOrderByCreatedAtDesc(String merchantId);
}

