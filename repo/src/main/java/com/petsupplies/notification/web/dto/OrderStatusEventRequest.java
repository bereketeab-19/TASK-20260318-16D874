package com.petsupplies.notification.web.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusEventRequest(
    @NotBlank String orderRef,
    @NotBlank String status
) {}
