package com.petsupplies.cooking.service;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.cooking.domain.CookingProcess;
import com.petsupplies.cooking.domain.CookingStatus;
import com.petsupplies.cooking.domain.CookingStep;
import com.petsupplies.cooking.domain.CookingTimer;
import com.petsupplies.cooking.repo.CookingProcessRepository;
import com.petsupplies.cooking.repo.CookingStepRepository;
import com.petsupplies.cooking.repo.CookingTimerRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CookingService {
  private static final Logger log = LoggerFactory.getLogger(CookingService.class);
  private final CookingProcessRepository processRepository;
  private final CookingStepRepository stepRepository;
  private final CookingTimerRepository timerRepository;
  private final AuditService auditService;
  private final Clock clock;

  public CookingService(
      CookingProcessRepository processRepository,
      CookingStepRepository stepRepository,
      CookingTimerRepository timerRepository,
      AuditService auditService,
      Clock clock
  ) {
    this.processRepository = processRepository;
    this.stepRepository = stepRepository;
    this.timerRepository = timerRepository;
    this.auditService = auditService;
    this.clock = clock;
  }

  @Transactional
  public CookingProcess createProcess(String merchantId, Long userId) {
    CookingProcess p = new CookingProcess();
    p.setMerchantId(merchantId);
    p.setUserId(userId);
    p.setStatus(CookingStatus.ACTIVE);
    p.setCurrentStepIndex(0);
    Instant now = Instant.now(clock);
    p.setLastCheckpointAt(now);
    p.setCreatedAt(now);
    p.setUpdatedAt(now);
    CookingProcess saved = processRepository.save(p);
    auditService.record("COOKING_PROCESS_CREATED", Map.of("merchantId", merchantId, "processId", saved.getId()), userId.toString(), null);
    return saved;
  }

  @Transactional
  public CookingProcess checkpoint(String merchantId, Long processId, int currentStepIndex, CookingStatus status, Long actorUserId) {
    CookingProcess p = processRepository.findByIdAndMerchantId(processId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    Instant now = Instant.now(clock);
    p.setCurrentStepIndex(currentStepIndex);
    p.setStatus(status);
    p.setLastCheckpointAt(now);
    p.setUpdatedAt(now);

    auditService.record(
        "COOKING_CHECKPOINT_SAVED",
        Map.of("merchantId", merchantId, "processId", processId, "currentStepIndex", currentStepIndex, "status", status.name()),
        actorUserId == null ? null : actorUserId.toString(),
        null
    );
    return p;
  }

  @Transactional(readOnly = true)
  public CookingProcess requireProcess(String merchantId, Long processId) {
    return processRepository.findByIdAndMerchantId(processId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
  }

  @Transactional(readOnly = true)
  public java.util.List<CookingTimer> listTimers(String merchantId, Long processId) {
    CookingProcess p = requireProcess(merchantId, processId);
    return timerRepository.findAllByProcess_Id(p.getId());
  }

  @Transactional
  public CookingTimer startTimer(String merchantId, Long processId, String label, Duration duration) {
    CookingProcess p = requireProcess(merchantId, processId);
    CookingTimer t = new CookingTimer();
    t.setProcess(p);
    t.setLabel(label);
    t.setStartTimestamp(Instant.now(clock));
    t.setDurationSeconds((int) duration.getSeconds());
    t.setCreatedAt(Instant.now(clock));
    return timerRepository.save(t);
  }

  public long remainingSeconds(CookingTimer timer) {
    long elapsed = Duration.between(timer.getStartTimestamp(), Instant.now(clock)).getSeconds();
    long remaining = timer.getDurationSeconds() - elapsed;
    return Math.max(0, remaining);
  }

  @Transactional
  public CookingStep addStep(String merchantId, Long processId, String instruction, Long actorUserId) {
    CookingProcess p = requireProcess(merchantId, processId);
    int nextIndex = stepRepository.maxStepIndex(processId) + 1;
    CookingStep s = new CookingStep();
    s.setProcess(p);
    s.setStepIndex(nextIndex);
    s.setInstruction(instruction);
    s.setCreatedAt(Instant.now(clock));
    CookingStep saved = stepRepository.save(s);
    markCheckpoint(p);
    auditService.record(
        "COOKING_STEP_ADDED",
        Map.of("merchantId", merchantId, "processId", processId, "stepId", saved.getId(), "stepIndex", nextIndex),
        actorUserId == null ? null : actorUserId.toString(),
        null
    );
    return saved;
  }

  @Transactional(readOnly = true)
  public java.util.List<CookingStep> listSteps(String merchantId, Long processId) {
    requireProcess(merchantId, processId);
    return stepRepository.findAllByProcess_IdOrderByStepIndexAsc(processId);
  }

  @Transactional
  public CookingStep completeStep(String merchantId, Long stepId, Long actorUserId) {
    CookingStep s = stepRepository.findByIdAndProcess_MerchantId(stepId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    s.setCompletedAt(Instant.now(clock));
    markCheckpoint(s.getProcess());
    auditService.record(
        "COOKING_STEP_COMPLETED",
        Map.of("merchantId", merchantId, "processId", s.getProcess().getId(), "stepId", stepId),
        actorUserId == null ? null : actorUserId.toString(),
        null
    );
    return stepRepository.save(s);
  }

  @Transactional
  public CookingStep scheduleStepReminder(String merchantId, Long stepId, Instant reminderAt, Long actorUserId) {
    CookingStep s = stepRepository.findByIdAndProcess_MerchantId(stepId, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    s.setReminderFireAt(reminderAt);
    markCheckpoint(s.getProcess());
    auditService.record(
        "COOKING_STEP_REMINDER_SCHEDULED",
        Map.of("merchantId", merchantId, "stepId", stepId, "reminderAt", reminderAt.toString()),
        actorUserId == null ? null : actorUserId.toString(),
        null
    );
    return stepRepository.save(s);
  }

  private void markCheckpoint(CookingProcess process) {
    Instant now = Instant.now(clock);
    process.setLastCheckpointAt(now);
    process.setUpdatedAt(now);
    processRepository.save(process);
    log.debug("Cooking checkpoint updated processId={}", process.getId());
  }
}

