package com.petsupplies.cooking.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StartTimerRequest(
    @NotBlank String label,
    @Positive int durationSeconds
) {}
