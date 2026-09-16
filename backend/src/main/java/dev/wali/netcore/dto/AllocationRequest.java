package dev.wali.netcore.dto;

import jakarta.validation.constraints.NotNull;

public record AllocationRequest(
        @NotNull Long networkInterfaceId
) {
}
