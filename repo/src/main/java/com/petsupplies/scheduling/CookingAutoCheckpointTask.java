package com.petsupplies.scheduling;

import com.petsupplies.cooking.domain.CookingProcess;
import com.petsupplies.cooking.domain.CookingStatus;
import com.petsupplies.cooking.repo.CookingProcessRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists cooking progress periodically (30s) for active processes so client disconnects lose less state.
 */
@Component
public class CookingAutoCheckpointTask {
  private static final Logger log = LoggerFactory.getLogger(CookingAutoCheckpointTask.class);
  private final CookingProcessRepository processRepository;
  private final Clock clock;

  public CookingAutoCheckpointTask(CookingProcessRepository processRepository, Clock clock) {
    this.processRepository = processRepository;
    this.clock = clock;
  }

  @Scheduled(fixedRate = 30_000)
  @Transactional
  public void autosaveActiveProcesses() {
    Instant now = Instant.now(clock);
    List<CookingProcess> active = processRepository.findAllByStatus(CookingStatus.ACTIVE);
    for (CookingProcess p : active) {
      p.setLastCheckpointAt(now);
      p.setUpdatedAt(now);
    }
    processRepository.saveAll(active);
    if (!active.isEmpty()) {
      log.debug("Cooking autosave touched {} active processes", active.size());
    }
  }
}
