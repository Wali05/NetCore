package dev.wali.netcore.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final HealthResponse HEALTH_UP = new HealthResponse("UP", "NetCore");

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(HEALTH_UP);
    }
}
