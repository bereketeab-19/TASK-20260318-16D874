package com.petsupplies.product.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAttributeDefinitionRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 120) String label
) {}
