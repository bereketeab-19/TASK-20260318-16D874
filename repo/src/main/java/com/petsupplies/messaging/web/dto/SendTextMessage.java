package com.petsupplies.messaging.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendTextMessage(
    @NotNull Long sessionId,
    @NotBlank String content
) {}

