package dev.wali.netcore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateNetworkInterfaceRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank
        @Pattern(
                regexp = "(?i)^[0-9a-f]{2}(:[0-9a-f]{2}){5}$",
                message = "must use six colon-separated octets"
        )
        String macAddress
) {
}
