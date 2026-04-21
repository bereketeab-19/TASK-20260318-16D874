package com.petsupplies.reporting.repo;

import com.petsupplies.reporting.domain.DailyInventoryReport;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyInventoryReportRepository extends JpaRepository<DailyInventoryReport, Long> {
  Optional<DailyInventoryReport> findByMerchantIdAndReportDate(String merchantId, LocalDate reportDate);

  List<DailyInventoryReport> findByMerchantIdOrderByReportDateDesc(String merchantId, Pageable pageable);
}

