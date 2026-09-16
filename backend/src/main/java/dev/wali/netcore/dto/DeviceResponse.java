package dev.wali.netcore.dto;

import dev.wali.netcore.domain.DeviceType;

import java.time.Instant;

public record DeviceResponse(
        Long id,
        String name,
        DeviceType type,
        Instant createdAt
) {
}
