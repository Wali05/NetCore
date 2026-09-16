package dev.wali.netcore.subnet;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "subnets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subnet_network_prefix",
                        columnNames = {"network_address", "prefix_length"}
                )
        }
)
public class Subnet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "network_address", nullable = false, length = 15)
    private String networkAddress;

    @Column(name = "prefix_length", nullable = false)
    private int prefixLength;

    @Column(name = "gateway_address", length = 15)
    private String gatewayAddress;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Subnet() {
        // Required by JPA
    }

    public Subnet(
            String networkAddress,
            int prefixLength,
            String gatewayAddress
    ) {
        long suppliedAddress = Ipv4AddressCalculator.toNumeric(networkAddress);
        long canonicalAddress = Ipv4AddressCalculator.networkAddress(
                suppliedAddress,
                prefixLength
        );

        this.networkAddress = Ipv4AddressCalculator.toAddress(canonicalAddress);
        this.prefixLength = prefixLength;
        setGatewayAddress(gatewayAddress);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getNetworkAddress() {
        return networkAddress;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public String getGatewayAddress() {
        return gatewayAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setGatewayAddress(String gatewayAddress) {
        if (gatewayAddress == null) {
            this.gatewayAddress = null;
            return;
        }

        long numericGateway = Ipv4AddressCalculator.toNumeric(gatewayAddress);
        String canonicalGateway = Ipv4AddressCalculator.toAddress(numericGateway);

        if (!Ipv4AddressCalculator.belongsToSubnet(
                canonicalGateway,
                networkAddress,
                prefixLength
        )) {
            throw new IllegalArgumentException("Gateway address must belong to the subnet");
        }

        long networkNumeric = Ipv4AddressCalculator.toNumeric(networkAddress);
        long broadcastNumeric = Ipv4AddressCalculator.broadcastAddress(
                networkNumeric,
                prefixLength
        );
        if (numericGateway == networkNumeric || numericGateway == broadcastNumeric) {
            throw new IllegalArgumentException("Gateway address must be a usable host address");
        }

        this.gatewayAddress = canonicalGateway;
    }
}
