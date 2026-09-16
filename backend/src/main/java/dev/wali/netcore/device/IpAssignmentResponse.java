package dev.wali.netcore.device;

import java.time.Instant;

public record IpAssignmentResponse(
        Long id,
        Long subnetId,
        String address,
        Instant allocatedAt
) {
}
