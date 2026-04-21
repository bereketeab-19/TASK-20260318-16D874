package com.petsupplies.auditing.repo;

import com.petsupplies.auditing.domain.AuditLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  // Intentionally no update/delete custom methods; append-only usage enforced in service layer + DB grants.

  Optional<AuditLog> findFirstByActionOrderByIdDesc(String action);
}

