package dev.wali.netcore.allocation;

import dev.wali.netcore.device.NetworkInterface;
import dev.wali.netcore.device.NetworkInterfaceNotFoundException;
import dev.wali.netcore.device.NetworkInterfaceRepository;
import dev.wali.netcore.subnet.IpAddress;
import dev.wali.netcore.subnet.IpAddressNotFoundException;
import dev.wali.netcore.subnet.IpAddressRepository;
import dev.wali.netcore.subnet.IpAddressStatus;
import dev.wali.netcore.subnet.Ipv4AddressCalculator;
import dev.wali.netcore.subnet.SubnetNotFoundException;
import dev.wali.netcore.subnet.SubnetRepository;

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
        lockSubnet(subnetId);
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
        lockSubnet(subnetId);
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
        lockSubnet(subnetId);
        String canonicalAddress = canonicalAddress(address);
        IpAddress ipAddress = ipAddressRepository
                .findBySubnetIdAndAddress(subnetId, canonicalAddress)
                .orElseThrow(() -> new IpAddressNotFoundException(subnetId, canonicalAddress));

        requireStatus(ipAddress, IpAddressStatus.ALLOCATED, "released");
        ipAddress.release();

        return ipAddress;
    }

    private void lockSubnet(Long subnetId) {
        if (subnetId == null) {
            throw new SubnetNotFoundException(subnetId);
        }

        // One lock per pool keeps selection and state changes in the same order.
        subnetRepository.findByIdForAllocation(subnetId)
                .orElseThrow(() -> new SubnetNotFoundException(subnetId));
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
