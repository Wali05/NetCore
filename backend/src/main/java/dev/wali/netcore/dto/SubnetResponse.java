package dev.wali.netcore.dto;

import java.time.Instant;

public record SubnetResponse(
        Long id,
        String networkAddress,
        int prefixLength,
        String cidr,
        String gatewayAddress,
        long totalAddresses,
        long availableAddresses,
        long allocatedAddresses,
        long reservedAddresses,
        double utilizationPercent,
        Instant createdAt
) {
}
