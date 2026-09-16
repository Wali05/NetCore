package dev.wali.netcore.allocation;

import jakarta.validation.constraints.NotNull;

public record AllocationRequest(
        @NotNull Long networkInterfaceId
) {
}
