package dev.wali.netcore.dto;

import java.time.Instant;

public record IpAssignmentResponse(
        Long id,
        Long subnetId,
        String address,
        Instant allocatedAt
) {
}
