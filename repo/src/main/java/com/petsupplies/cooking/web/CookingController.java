package com.petsupplies.cooking.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.cooking.domain.CookingProcess;
import com.petsupplies.cooking.service.CookingService;
import com.petsupplies.cooking.web.dto.CheckpointRequest;
import com.petsupplies.cooking.web.dto.CreateCookingStepRequest;
import com.petsupplies.cooking.web.dto.ScheduleStepReminderRequest;
import com.petsupplies.cooking.web.dto.StartTimerRequest;
import com.petsupplies.user.security.SecurityUser;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class CookingController {
  private final CurrentPrincipal currentPrincipal;
  private final CookingService cookingService;

  public CookingController(CurrentPrincipal currentPrincipal, CookingService cookingService) {
    this.currentPrincipal = currentPrincipal;
    this.cookingService = cookingService;
  }

  @PostMapping("/cooking/processes")
  public Map<String, Object> create(Authentication authentication) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    Long userId = user.getUserId();
    CookingProcess p = cookingService.createProcess(merchantId, userId);
    return Map.of("id", p.getId(), "status", p.getStatus().name(), "currentStepIndex", p.getCurrentStepIndex());
  }

  @PostMapping("/cooking/checkpoint")
  public Map<String, Object> checkpoint(Authentication authentication, @Valid @RequestBody CheckpointRequest body) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    CookingProcess p = cookingService.checkpoint(merchantId, body.processId(), body.currentStepIndex(), body.status(), user.getUserId());
    return Map.of(
        "id", p.getId(),
        "status", p.getStatus().name(),
        "currentStepIndex", p.getCurrentStepIndex(),
        "lastCheckpointAt", p.getLastCheckpointAt()
    );
  }

  @PostMapping("/cooking/processes/{id}/timers")
  public Map<String, Object> startTimer(
      Authentication authentication,
      @PathVariable("id") Long processId,
      @Valid @RequestBody StartTimerRequest body
  ) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var t = cookingService.startTimer(merchantId, processId, body.label(), Duration.ofSeconds(body.durationSeconds()));
    return Map.of(
        "id", t.getId(),
        "label", t.getLabel(),
        "startTimestamp", t.getStartTimestamp(),
        "durationSeconds", t.getDurationSeconds()
    );
  }

  @PostMapping("/cooking/processes/{id}/steps")
  public Map<String, Object> addStep(
      Authentication authentication,
      @PathVariable("id") Long processId,
      @Valid @RequestBody CreateCookingStepRequest body
  ) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var s = cookingService.addStep(merchantId, processId, body.instruction(), user.getUserId());
    return Map.of("id", s.getId(), "stepIndex", s.getStepIndex(), "instruction", s.getInstruction());
  }

  @PostMapping("/cooking/steps/{stepId}/complete")
  public Map<String, Object> completeStep(Authentication authentication, @PathVariable("stepId") Long stepId) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var s = cookingService.completeStep(merchantId, stepId, user.getUserId());
    return Map.of("id", s.getId(), "completedAt", s.getCompletedAt());
  }

  @PostMapping("/cooking/steps/{stepId}/reminder")
  public Map<String, Object> scheduleReminder(
      Authentication authentication,
      @PathVariable("stepId") Long stepId,
      @Valid @RequestBody ScheduleStepReminderRequest body
  ) {
    SecurityUser user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var s = cookingService.scheduleStepReminder(merchantId, stepId, body.reminderAt(), user.getUserId());
    return Map.of("id", s.getId(), "reminderFireAt", s.getReminderFireAt());
  }

  @GetMapping("/cooking/processes/{id}")
  public Map<String, Object> get(Authentication authentication, @PathVariable("id") Long id) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    CookingProcess p = cookingService.requireProcess(merchantId, id);
    List<Map<String, Object>> steps = cookingService.listSteps(merchantId, id).stream()
        .map(s -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", s.getId());
          m.put("stepIndex", s.getStepIndex());
          m.put("instruction", s.getInstruction());
          m.put("completedAt", s.getCompletedAt());
          m.put("reminderFireAt", s.getReminderFireAt());
          return m;
        })
        .toList();
    var timers = cookingService.listTimers(merchantId, id).stream()
        .map(t -> Map.of(
            "id", t.getId(),
            "label", t.getLabel(),
            "startTimestamp", t.getStartTimestamp(),
            "durationSeconds", t.getDurationSeconds(),
            "remainingSeconds", cookingService.remainingSeconds(t)
        ))
        .toList();
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", p.getId());
    out.put("status", p.getStatus().name());
    out.put("currentStepIndex", p.getCurrentStepIndex());
    out.put("lastCheckpointAt", p.getLastCheckpointAt());
    out.put("steps", steps);
    out.put("timers", timers);
    return out;
  }
}

