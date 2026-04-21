package com.petsupplies.product.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
    @NotBlank @Size(max = 64) String productCode,
    @NotBlank @Size(max = 200) String name
) {}

