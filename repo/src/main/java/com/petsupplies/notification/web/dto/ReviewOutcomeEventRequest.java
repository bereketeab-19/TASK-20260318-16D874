package com.petsupplies.notification.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewOutcomeEventRequest(
    @NotBlank String reviewRef,
    @NotBlank String outcome
) {}
