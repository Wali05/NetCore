package dev.wali.netcore.dto;

import dev.wali.netcore.domain.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDeviceRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull DeviceType type
) {
}
