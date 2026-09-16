package dev.wali.netcore.device;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(
        name = "network_interfaces",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_network_interface_mac",
                        columnNames = "mac_address"
                ),
                @UniqueConstraint(
                        name = "uk_network_interface_device_name",
                        columnNames = {"device_id", "name"}
                )
        }
)
public class NetworkInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "mac_address", nullable = false, length = 17)
    private String macAddress;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected NetworkInterface() {
        // Required by JPA
    }

    public NetworkInterface(Device device, String name, String macAddress) {
        if (device == null) {
            throw new IllegalArgumentException("Device is required");
        }

        this.device = device;
        setName(name);
        setMacAddress(macAddress);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Device getDevice() {
        return device;
    }

    public String getName() {
        return name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Interface name is required");
        }

        this.name = name.trim();
    }

    public void setMacAddress(String macAddress) {
        if (macAddress == null || !macAddress.matches("(?i)^[0-9a-f]{2}(:[0-9a-f]{2}){5}$")) {
            throw new IllegalArgumentException("MAC address must use six colon-separated octets");
        }

        this.macAddress = macAddress.toUpperCase(Locale.ROOT);
    }
}
