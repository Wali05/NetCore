package dev.wali.netcore.subnet;

import dev.wali.netcore.device.Device;
import dev.wali.netcore.device.DeviceRepository;
import dev.wali.netcore.device.DeviceType;
import dev.wali.netcore.device.NetworkInterface;
import dev.wali.netcore.device.NetworkInterfaceRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("local")
class PersistenceWorkflowTests {

    @Autowired
    private SubnetPoolPersistenceService subnetPoolPersistenceService;

    @Autowired
    private SubnetRepository subnetRepository;

    @Autowired
    private IpAddressRepository ipAddressRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private NetworkInterfaceRepository networkInterfaceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        ipAddressRepository.deleteAllInBatch();
        networkInterfaceRepository.deleteAllInBatch();
        deviceRepository.deleteAllInBatch();
        subnetRepository.deleteAllInBatch();
    }

    @Test
    void persistsAndReloadsCompleteSlash24Pool() {
        Subnet savedSubnet = subnetPoolPersistenceService.createSubnetWithPool(
                new Subnet("192.168.10.25", 24, "192.168.10.1")
        );

        assertNotNull(savedSubnet.getId());

        Subnet reloadedSubnet = subnetRepository.findById(savedSubnet.getId()).orElseThrow();
        List<IpAddress> reloadedPool =
                ipAddressRepository.findAllBySubnetIdOrderByAddressNumericAsc(savedSubnet.getId());

        assertEquals("192.168.10.0", reloadedSubnet.getNetworkAddress());
        assertEquals(256, reloadedPool.size());
        assertEquals("192.168.10.0", reloadedPool.getFirst().getAddress());
        assertEquals(IpAddressStatus.RESERVED, reloadedPool.getFirst().getStatus());
        assertEquals("192.168.10.255", reloadedPool.getLast().getAddress());
        assertEquals(IpAddressStatus.RESERVED, reloadedPool.getLast().getStatus());
        assertEquals(
                253,
                ipAddressRepository.countBySubnetIdAndStatus(
                        savedSubnet.getId(),
                        IpAddressStatus.AVAILABLE
                )
        );
        assertEquals(
                3,
                ipAddressRepository.countBySubnetIdAndStatus(
                        savedSubnet.getId(),
                        IpAddressStatus.RESERVED
                )
        );
    }

    @Test
    void storesIpAddressStatusAsReadableText() {
        Subnet subnet = subnetPoolPersistenceService.createSubnetWithPool(
                new Subnet("10.20.30.0", 30, "10.20.30.1")
        );

        String storedStatus = jdbcTemplate.queryForObject(
                "select status from ip_addresses where subnet_id = ? and address = ?",
                String.class,
                subnet.getId(),
                "10.20.30.0"
        );

        assertEquals("RESERVED", storedStatus);
    }

    @Test
    void supportsPagedAndStatusFilteredPoolQueries() {
        Subnet subnet = subnetPoolPersistenceService.createSubnetWithPool(
                new Subnet("172.16.8.0", 29, null)
        );

        Page<IpAddress> availablePage = ipAddressRepository.findAllBySubnetIdAndStatus(
                subnet.getId(),
                IpAddressStatus.AVAILABLE,
                PageRequest.of(0, 3, Sort.by("addressNumeric"))
        );

        assertEquals(6, availablePage.getTotalElements());
        assertEquals(3, availablePage.getContent().size());
        assertEquals("172.16.8.1", availablePage.getContent().getFirst().getAddress());
    }

    @Test
    void persistsAndReloadsDeviceInterfaceIpAssignment() {
        Subnet subnet = subnetPoolPersistenceService.createSubnetWithPool(
                new Subnet("192.168.50.0", 30, null)
        );
        Device device = deviceRepository.save(
                new Device("compute-node-04", DeviceType.SERVER)
        );
        NetworkInterface networkInterface = networkInterfaceRepository.save(
                new NetworkInterface(device, "eth0", "AA:BB:CC:DD:EE:04")
        );
        IpAddress ipAddress = ipAddressRepository
                .findBySubnetIdAndAddress(subnet.getId(), "192.168.50.1")
                .orElseThrow();

        ipAddress.allocateTo(networkInterface);
        IpAddress savedIpAddress = ipAddressRepository.save(ipAddress);

        IpAddress reloadedIpAddress = ipAddressRepository
                .findWithAssignmentById(savedIpAddress.getId())
                .orElseThrow();

        assertEquals(IpAddressStatus.ALLOCATED, reloadedIpAddress.getStatus());
        assertNotNull(reloadedIpAddress.getAllocatedAt());
        assertEquals("eth0", reloadedIpAddress.getNetworkInterface().getName());
        assertEquals(
                "compute-node-04",
                reloadedIpAddress.getNetworkInterface().getDevice().getName()
        );
        assertEquals("192.168.50.0", reloadedIpAddress.getSubnet().getNetworkAddress());
    }

    @Test
    void rollsBackSubnetWhenPoolGenerationFails() {
        Subnet unsupportedSubnet = new Subnet("10.0.0.0", 23, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> subnetPoolPersistenceService.createSubnetWithPool(unsupportedSubnet)
        );

        assertEquals(0, subnetRepository.count());
        assertEquals(0, ipAddressRepository.count());
    }
}
