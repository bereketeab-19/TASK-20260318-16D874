package com.petsupplies.auditing.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.petsupplies.auditing.domain.CriticalOperationType;
import jakarta.validation.constraints.NotNull;

public record ApprovalRequestDto(
    @NotNull CriticalOperationType operationType,
    @NotNull JsonNode payload
) {}

