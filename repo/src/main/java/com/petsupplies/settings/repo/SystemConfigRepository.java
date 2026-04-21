package com.petsupplies.settings.repo;

import com.petsupplies.settings.domain.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {}
