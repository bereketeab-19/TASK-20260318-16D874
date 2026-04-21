package com.petsupplies.messaging.repo;

import com.petsupplies.messaging.domain.Attachment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
  Optional<Attachment> findByMerchantIdAndSha256(String merchantId, String sha256);

  Optional<Attachment> findByIdAndMerchantId(Long id, String merchantId);
}

