package com.petsupplies.cooking.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCookingStepRequest(@NotBlank String instruction) {}
