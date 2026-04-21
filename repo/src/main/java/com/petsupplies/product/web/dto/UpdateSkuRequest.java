package com.petsupplies.product.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateSkuRequest(
    @Min(0) Integer stockQuantity,
    @Size(max = 64) String barcode
) {}
