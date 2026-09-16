package dev.wali.netcore.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDeviceRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull DeviceType type
) {
}
