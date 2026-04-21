package com.petsupplies.cooking.web.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateTechniqueCardRequest(
    @Size(max = 200) String title,
    String body,
    List<@Size(max = 120) String> tags
) {}
