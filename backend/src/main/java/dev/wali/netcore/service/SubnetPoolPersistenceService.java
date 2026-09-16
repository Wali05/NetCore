package dev.wali.netcore.service;

import dev.wali.netcore.domain.IpAddress;
import dev.wali.netcore.domain.Subnet;
import dev.wali.netcore.repository.IpAddressRepository;
import dev.wali.netcore.repository.SubnetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubnetPoolPersistenceService {

    private final SubnetRepository subnetRepository;
    private final IpAddressRepository ipAddressRepository;
    private final IpPoolService ipPoolService;

    public SubnetPoolPersistenceService(
            SubnetRepository subnetRepository,
            IpAddressRepository ipAddressRepository,
            IpPoolService ipPoolService
    ) {
        this.subnetRepository = subnetRepository;
        this.ipAddressRepository = ipAddressRepository;
        this.ipPoolService = ipPoolService;
    }

    @Transactional
    public Subnet createSubnetWithPool(Subnet subnet) {
        if (subnet == null) {
            throw new IllegalArgumentException("Subnet is required");
        }

        if (subnet.getId() != null) {
            throw new IllegalArgumentException("Subnet must not already be persisted");
        }

        Subnet savedSubnet = subnetRepository.save(subnet);
        List<IpAddress> pool = ipPoolService.generatePool(savedSubnet);
        ipAddressRepository.saveAll(pool);

        return savedSubnet;
    }
}
