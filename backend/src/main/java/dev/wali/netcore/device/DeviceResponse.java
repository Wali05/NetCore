package dev.wali.netcore.device;

import java.time.Instant;

public record DeviceResponse(
        Long id,
        String name,
        DeviceType type,
        Instant createdAt
) {
}
