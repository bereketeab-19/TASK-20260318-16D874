package com.petsupplies.product.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSkuRequest(
    @NotNull Long productId,
    @NotBlank @Size(max = 64) String barcode,
    @Min(0) int stockQuantity
) {}

