package com.petsupplies.cooking.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ScheduleStepReminderRequest(@NotNull Instant reminderAt) {}
