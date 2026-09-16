package dev.wali.netcore.service;

import dev.wali.netcore.domain.IpAddress;
import dev.wali.netcore.domain.IpAddressStatus;
import dev.wali.netcore.domain.Ipv4AddressCalculator;
import dev.wali.netcore.domain.NetworkInterface;
import dev.wali.netcore.exception.IpAddressNotFoundException;
import dev.wali.netcore.exception.IpAddressStateConflictException;
import dev.wali.netcore.exception.NetworkInterfaceNotFoundException;
import dev.wali.netcore.exception.NoAvailableIpAddressException;
import dev.wali.netcore.exception.SubnetNotFoundException;
import dev.wali.netcore.repository.IpAddressRepository;
import dev.wali.netcore.repository.NetworkInterfaceRepository;
import dev.wali.netcore.repository.SubnetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IpAllocationService {

    private final SubnetRepository subnetRepository;
    private final IpAddressRepository ipAddressRepository;
    private final NetworkInterfaceRepository networkInterfaceRepository;

    public IpAllocationService(
            SubnetRepository subnetRepository,
            IpAddressRepository ipAddressRepository,
            NetworkInterfaceRepository networkInterfaceRepository
    ) {
        this.subnetRepository = subnetRepository;
        this.ipAddressRepository = ipAddressRepository;
        this.networkInterfaceRepository = networkInterfaceRepository;
    }

    @Transactional
    public IpAddress allocateSpecific(
            Long subnetId,
            String address,
            Long networkInterfaceId
    ) {
        requireSubnet(subnetId);
        NetworkInterface networkInterface = requireNetworkInterface(networkInterfaceId);
        String canonicalAddress = canonicalAddress(address);
        IpAddress ipAddress = ipAddressRepository
                .findBySubnetIdAndAddress(subnetId, canonicalAddress)
                .orElseThrow(() -> new IpAddressNotFoundException(subnetId, canonicalAddress));

        requireStatus(ipAddress, IpAddressStatus.AVAILABLE, "allocated");
        ipAddress.allocateTo(networkInterface);

        return ipAddress;
    }

    @Transactional
    public IpAddress allocateNextAvailable(
            Long subnetId,
            Long networkInterfaceId
    ) {
        requireSubnet(subnetId);
        NetworkInterface networkInterface = requireNetworkInterface(networkInterfaceId);

        // Numeric ordering gives callers a predictable lowest-address-first allocation.
        IpAddress ipAddress = ipAddressRepository
                .findFirstBySubnetIdAndStatusOrderByAddressNumericAsc(
                        subnetId,
                        IpAddressStatus.AVAILABLE
                )
                .orElseThrow(() -> new NoAvailableIpAddressException(subnetId));

        ipAddress.allocateTo(networkInterface);

        return ipAddress;
    }

    @Transactional
    public IpAddress release(Long subnetId, String address) {
        requireSubnet(subnetId);
        String canonicalAddress = canonicalAddress(address);
        IpAddress ipAddress = ipAddressRepository
                .findBySubnetIdAndAddress(subnetId, canonicalAddress)
                .orElseThrow(() -> new IpAddressNotFoundException(subnetId, canonicalAddress));

        requireStatus(ipAddress, IpAddressStatus.ALLOCATED, "released");
        ipAddress.release();

        return ipAddress;
    }

    private void requireSubnet(Long subnetId) {
        if (subnetId == null || !subnetRepository.existsById(subnetId)) {
            throw new SubnetNotFoundException(subnetId);
        }
    }

    private NetworkInterface requireNetworkInterface(Long networkInterfaceId) {
        if (networkInterfaceId == null) {
            throw new NetworkInterfaceNotFoundException(null);
        }

        return networkInterfaceRepository.findById(networkInterfaceId)
                .orElseThrow(() -> new NetworkInterfaceNotFoundException(networkInterfaceId));
    }

    private String canonicalAddress(String address) {
        return Ipv4AddressCalculator.toAddress(
                Ipv4AddressCalculator.toNumeric(address)
        );
    }

    private void requireStatus(
            IpAddress ipAddress,
            IpAddressStatus requiredStatus,
            String requestedOperation
    ) {
        if (ipAddress.getStatus() != requiredStatus) {
            throw new IpAddressStateConflictException(
                    ipAddress.getAddress(),
                    ipAddress.getStatus(),
                    requestedOperation
            );
        }
    }
}
