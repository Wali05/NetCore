package dev.wali.netcore.health;

public record HealthResponse(
        String status,
        String service
) {
}
