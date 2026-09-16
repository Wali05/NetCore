package dev.wali.netcore.device;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceType type;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Device() {
        // Required by JPA
    }

    public Device(String name, DeviceType type) {
        setName(name);
        setType(type);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DeviceType getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name is required");
        }

        this.name = name.trim();
    }

    public void setType(DeviceType type) {
        if (type == null) {
            throw new IllegalArgumentException("Device type is required");
        }

        this.type = type;
    }
}
