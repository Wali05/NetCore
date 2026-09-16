package dev.wali.netcore.subnet;

import dev.wali.netcore.device.NetworkInterface;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "ip_addresses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ip_address_subnet_address",
                        columnNames = {"subnet_id", "address"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_ip_address_subnet_status_numeric",
                        columnList = "subnet_id, status, address_numeric"
                )
        }
)
public class IpAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subnet_id", nullable = false)
    private Subnet subnet;

    @Column(nullable = false, length = 15)
    private String address;

    @Column(name = "address_numeric", nullable = false)
    private long addressNumeric;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IpAddressStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_interface_id")
    private NetworkInterface networkInterface;

    private Instant allocatedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected IpAddress() {
        // Required by JPA
    }

    public static IpAddress createAvailable(Subnet subnet, String address) {
        return new IpAddress(subnet, address, IpAddressStatus.AVAILABLE);
    }

    public static IpAddress createReserved(Subnet subnet, String address) {
        return new IpAddress(subnet, address, IpAddressStatus.RESERVED);
    }

    public IpAddress(
            Subnet subnet,
            String address
    ) {
        this(subnet, address, IpAddressStatus.AVAILABLE);
    }

    private IpAddress(
            Subnet subnet,
            String address,
            IpAddressStatus status
    ) {
        if (subnet == null) {
            throw new IllegalArgumentException("Subnet is required");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }

        if (status != IpAddressStatus.AVAILABLE && status != IpAddressStatus.RESERVED) {
            throw new IllegalArgumentException("Initial status must be AVAILABLE or RESERVED");
        }

        long numericAddress = Ipv4AddressCalculator.toNumeric(address);

        if (!Ipv4AddressCalculator.belongsToSubnet(
                address,
                subnet.getNetworkAddress(),
                subnet.getPrefixLength()
        )) {
            throw new IllegalArgumentException("IP address must belong to the subnet");
        }

        this.subnet = subnet;
        this.address = Ipv4AddressCalculator.toAddress(numericAddress);
        this.addressNumeric = numericAddress;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void allocateTo(NetworkInterface networkInterface) {
        if (status != IpAddressStatus.AVAILABLE) {
            throw new IllegalStateException("Only available IP addresses can be allocated");
        }

        if (networkInterface == null) {
            throw new IllegalArgumentException("Network interface is required");
        }

        this.networkInterface = networkInterface;
        this.status = IpAddressStatus.ALLOCATED;
        this.allocatedAt = Instant.now();
    }

    public void release() {
        if (status != IpAddressStatus.ALLOCATED) {
            throw new IllegalStateException("Only allocated IP addresses can be released");
        }

        this.networkInterface = null;
        this.status = IpAddressStatus.AVAILABLE;
        this.allocatedAt = null;
    }

    public Long getId() {
        return id;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    public String getAddress() {
        return address;
    }

    public long getAddressNumeric() {
        return addressNumeric;
    }

    public IpAddressStatus getStatus() {
        return status;
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public Instant getAllocatedAt() {
        return allocatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
