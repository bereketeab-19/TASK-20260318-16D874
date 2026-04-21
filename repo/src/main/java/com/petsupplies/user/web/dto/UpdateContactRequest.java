package com.petsupplies.user.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateContactRequest(@Size(max = 120) String contactPhone) {}
