package com.petsupplies.cooking.web.dto;

import com.petsupplies.cooking.domain.CookingStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckpointRequest(
    @NotNull Long processId,
    @Min(0) int currentStepIndex,
    @NotNull CookingStatus status
) {}

