package com.petsupplies.reporting.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomReportRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description,
    /** JSON with at least {@code template} (see api-spec). */
    @NotBlank String definitionJson,
    @Size(max = 120) String scheduleCron,
    @Size(max = 64) String scheduleTimezone
) {}
