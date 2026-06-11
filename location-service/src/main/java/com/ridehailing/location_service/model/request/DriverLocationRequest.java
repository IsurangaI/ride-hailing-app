package com.ridehailing.location_service.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DriverLocationRequest(
        @NotNull Double latitude,
        @NotNull Double longitude,
        boolean available
) {}