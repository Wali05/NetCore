package dev.wali.netcore.dto;

public record HealthResponse(
        String status,
        String service
) {
}
