package com.petsupplies.reporting.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateCustomReportRequest(
    @Size(max = 200) String name,
    @Size(max = 2000) String description,
    String definitionJson,
    @Size(max = 120) String scheduleCron,
    @Size(max = 64) String scheduleTimezone,
    Boolean active
) {}
