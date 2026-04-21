package com.petsupplies.achievement.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAchievementRequest(
    @NotNull Long userId,
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 120) String period,
    @NotBlank @Size(max = 120) String responsiblePerson,
    @NotBlank String conclusion
) {}

