package dev.wali.netcore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateSubnetRequest(
        @NotBlank String networkAddress,
        @Min(24) @Max(30) int prefixLength,
        String gatewayAddress
) {
}
