package com.petsupplies.messaging.web.dto;

import jakarta.validation.constraints.NotNull;

public record SendImageMessage(
    @NotNull Long sessionId,
    @NotNull Long attachmentId,
    String caption
) {}
