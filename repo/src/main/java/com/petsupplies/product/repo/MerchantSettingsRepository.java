package com.petsupplies.product.repo;

import com.petsupplies.product.domain.MerchantSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantSettingsRepository extends JpaRepository<MerchantSettings, String> {}
