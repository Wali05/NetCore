package dev.wali.netcore.dto;

import java.time.Instant;
import java.util.List;

public record NetworkInterfaceResponse(
        Long id,
        Long deviceId,
        String deviceName,
        String name,
        String macAddress,
        Instant createdAt,
        List<IpAssignmentResponse> assignedAddresses
) {
}
