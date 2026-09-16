package dev.wali.netcore.subnet;

import dev.wali.netcore.allocation.IpAllocationService;
import dev.wali.netcore.device.NetworkInterface;
import dev.wali.netcore.shared.PageResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubnetManagementService {

    private final SubnetRepository subnetRepository;
    private final IpAddressRepository ipAddressRepository;
    private final SubnetPoolPersistenceService subnetPoolPersistenceService;
    private final IpAllocationService ipAllocationService;

    public SubnetManagementService(
            SubnetRepository subnetRepository,
            IpAddressRepository ipAddressRepository,
            SubnetPoolPersistenceService subnetPoolPersistenceService,
            IpAllocationService ipAllocationService
    ) {
        this.subnetRepository = subnetRepository;
        this.ipAddressRepository = ipAddressRepository;
        this.subnetPoolPersistenceService = subnetPoolPersistenceService;
        this.ipAllocationService = ipAllocationService;
    }

    @Transactional
    public SubnetResponse createSubnet(
            String networkAddress,
            int prefixLength,
            String gatewayAddress
    ) {
        Subnet subnet = subnetPoolPersistenceService.createSubnetWithPool(
                new Subnet(networkAddress, prefixLength, gatewayAddress)
        );
        return toSubnetResponse(subnet);
    }

    @Transactional(readOnly = true)
    public List<SubnetResponse> listSubnets() {
        return subnetRepository.findAll(
                        Sort.by("networkAddress").ascending()
                                .and(Sort.by("prefixLength").ascending())
                ).stream()
                .map(this::toSubnetResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubnetResponse getSubnet(Long subnetId) {
        return toSubnetResponse(requireSubnet(subnetId));
    }

    @Transactional(readOnly = true)
    public PageResponse<IpAddressResponse> listAddresses(
            Long subnetId,
            IpAddressStatus status,
            int page,
            int size
    ) {
        requireSubnet(subnetId);
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by("addressNumeric").ascending()
        );
        Page<IpAddress> addresses = status == null
                ? ipAddressRepository.findAllBySubnetId(subnetId, pageRequest)
                : ipAddressRepository.findAllBySubnetIdAndStatus(subnetId, status, pageRequest);

        return PageResponse.from(addresses, this::toIpAddressResponse);
    }

    @Transactional
    public IpAddressResponse allocateSpecific(
            Long subnetId,
            String address,
            Long networkInterfaceId
    ) {
        return toIpAddressResponse(
                ipAllocationService.allocateSpecific(
                        subnetId,
                        address,
                        networkInterfaceId
                )
        );
    }

    @Transactional
    public IpAddressResponse allocateNextAvailable(
            Long subnetId,
            Long networkInterfaceId
    ) {
        return toIpAddressResponse(
                ipAllocationService.allocateNextAvailable(
                        subnetId,
                        networkInterfaceId
                )
        );
    }

    @Transactional
    public IpAddressResponse release(Long subnetId, String address) {
        return toIpAddressResponse(ipAllocationService.release(subnetId, address));
    }

    private Subnet requireSubnet(Long subnetId) {
        if (subnetId == null) {
            throw new SubnetNotFoundException(null);
        }

        return subnetRepository.findById(subnetId)
                .orElseThrow(() -> new SubnetNotFoundException(subnetId));
    }

    private SubnetResponse toSubnetResponse(Subnet subnet) {
        long available = count(subnet, IpAddressStatus.AVAILABLE);
        long allocated = count(subnet, IpAddressStatus.ALLOCATED);
        long reserved = count(subnet, IpAddressStatus.RESERVED);
        long total = available + allocated + reserved;

        // Reserved addresses cannot be allocated, so utilization is based on the usable pool.
        long usable = available + allocated;
        double utilization = usable == 0 ? 0 : allocated * 100.0 / usable;

        return new SubnetResponse(
                subnet.getId(),
                subnet.getNetworkAddress(),
                subnet.getPrefixLength(),
                subnet.getNetworkAddress() + "/" + subnet.getPrefixLength(),
                subnet.getGatewayAddress(),
                total,
                available,
                allocated,
                reserved,
                utilization,
                subnet.getCreatedAt()
        );
    }

    private long count(Subnet subnet, IpAddressStatus status) {
        return ipAddressRepository.countBySubnetIdAndStatus(subnet.getId(), status);
    }

    private IpAddressResponse toIpAddressResponse(IpAddress ipAddress) {
        NetworkInterface networkInterface = ipAddress.getNetworkInterface();

        return new IpAddressResponse(
                ipAddress.getId(),
                ipAddress.getSubnet().getId(),
                ipAddress.getAddress(),
                ipAddress.getStatus(),
                networkInterface == null ? null : networkInterface.getId(),
                networkInterface == null ? null : networkInterface.getName(),
                networkInterface == null ? null : networkInterface.getDevice().getId(),
                networkInterface == null ? null : networkInterface.getDevice().getName(),
                ipAddress.getAllocatedAt()
        );
    }
}
