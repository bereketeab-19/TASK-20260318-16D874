package com.petsupplies.auditing.repo;

import com.petsupplies.auditing.domain.PendingApproval;
import com.petsupplies.auditing.domain.PendingApprovalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingApprovalRepository extends JpaRepository<PendingApproval, Long> {
  Optional<PendingApproval> findByIdAndStatus(Long id, PendingApprovalStatus status);
  List<PendingApproval> findTop100ByStatusOrderByCreatedAtDesc(PendingApprovalStatus status);
}

