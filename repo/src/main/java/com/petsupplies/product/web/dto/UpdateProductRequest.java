package com.petsupplies.product.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
    @Size(max = 200) String name,
    Long categoryId,
    Long brandId
) {}
