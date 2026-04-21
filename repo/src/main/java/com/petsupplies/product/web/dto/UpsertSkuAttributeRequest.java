package com.petsupplies.product.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertSkuAttributeRequest(
    @NotNull Long attributeDefinitionId,
    @NotBlank String value
) {}
