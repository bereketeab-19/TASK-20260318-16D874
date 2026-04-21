package com.petsupplies.notification.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertNotificationSubscriptionRequest(
    @NotBlank String eventType,
    @NotNull Boolean enabled
) {}
