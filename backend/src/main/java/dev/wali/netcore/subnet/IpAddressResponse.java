package dev.wali.netcore.subnet;

import java.time.Instant;

public record IpAddressResponse(
        Long id,
        Long subnetId,
        String address,
        IpAddressStatus status,
        Long networkInterfaceId,
        String networkInterfaceName,
        Long deviceId,
        String deviceName,
        Instant allocatedAt
) {
}
