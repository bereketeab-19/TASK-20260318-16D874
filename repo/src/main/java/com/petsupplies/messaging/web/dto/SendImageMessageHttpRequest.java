package com.petsupplies.messaging.web.dto;

import jakarta.validation.constraints.NotNull;

public record SendImageMessageHttpRequest(
    @NotNull Long attachmentId,
    String caption
) {}
