package com.petsupplies.cooking.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateTechniqueCardRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank String body,
    List<@NotBlank @Size(max = 120) String> tags
) {}
