package com.petsupplies.product.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateSkuRequest(
    Integer stockQuantity,
    @Size(max = 64) String barcode
) {}
