package dev.wali.netcore.allocation;

import dev.wali.netcore.device.Device;
import dev.wali.netcore.device.DeviceRepository;
import dev.wali.netcore.device.DeviceType;
import dev.wali.netcore.device.NetworkInterface;
import dev.wali.netcore.device.NetworkInterfaceNotFoundException;
import dev.wali.netcore.device.NetworkInterfaceRepository;
import dev.wali.netcore.subnet.IpAddress;
import dev.wali.netcore.subnet.IpAddressNotFoundException;
import dev.wali.netcore.subnet.IpAddressRepository;
import dev.wali.netcore.subnet.IpAddressStatus;
import dev.wali.netcore.subnet.Subnet;
import dev.wali.netcore.subnet.SubnetNotFoundException;
import dev.wali.netcore.subnet.SubnetPoolPersistenceService;
import dev.wali.netcore.subnet.SubnetRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("local")
class IpAllocationServiceTests {

    @Autowired
    private IpAllocationService ipAllocationService;

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

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        ipAddressRepository.deleteAllInBatch();
        networkInterfaceRepository.deleteAllInBatch();
        deviceRepository.deleteAllInBatch();
        subnetRepository.deleteAllInBatch();
    }

    @Test
    void allocatesSpecificAvailableAddressToInterface() {
        Subnet subnet = createSubnet("192.168.10.0", 29, "192.168.10.1");
        NetworkInterface networkInterface = createInterface(
                "compute-node-01",
                "AA:BB:CC:DD:EE:01"
        );

        IpAddress allocated = ipAllocationService.allocateSpecific(
                subnet.getId(),
                "192.168.10.3",
                networkInterface.getId()
        );

        IpAddress reloaded = ipAddressRepository
                .findWithAssignmentById(allocated.getId())
                .orElseThrow();
        assertEquals(IpAddressStatus.ALLOCATED, reloaded.getStatus());
        assertEquals(networkInterface.getId(), reloaded.getNetworkInterface().getId());
        assertNotNull(reloaded.getAllocatedAt());
    }

    @Test
    void allocatesAvailableAddressesInNumericOrder() {
        Subnet subnet = createSubnet("192.168.20.0", 29, "192.168.20.1");
        NetworkInterface firstInterface = createInterface(
                "compute-node-01",
                "AA:BB:CC:DD:EE:01"
        );
        NetworkInterface secondInterface = createInterface(
                "compute-node-02",
                "AA:BB:CC:DD:EE:02"
        );

        IpAddress first = ipAllocationService.allocateNextAvailable(
                subnet.getId(),
                firstInterface.getId()
        );
        IpAddress second = ipAllocationService.allocateNextAvailable(
                subnet.getId(),
                secondInterface.getId()
        );

        assertEquals("192.168.20.2", first.getAddress());
        assertEquals("192.168.20.3", second.getAddress());
    }

    @Test
    void rejectsReservedAddressAllocation() {
        Subnet subnet = createSubnet("192.168.30.0", 30, "192.168.30.1");
        NetworkInterface networkInterface = createInterface(
                "compute-node-01",
                "AA:BB:CC:DD:EE:01"
        );

        IpAddressStateConflictException exception = assertThrows(
                IpAddressStateConflictException.class,
                () -> ipAllocationService.allocateSpecific(
                        subnet.getId(),
                        "192.168.30.1",
                        networkInterface.getId()
                )
        );

        assertEquals(IpAddressStatus.RESERVED, exception.getStatus());
    }

    @Test
    void rejectsAddressThatIsAlreadyAllocated() {
        Subnet subnet = createSubnet("192.168.40.0", 30, null);
        NetworkInterface firstInterface = createInterface(
                "compute-node-01",
                "AA:BB:CC:DD:EE:01"
        );
        NetworkInterface secondInterface = createInterface(
                "compute-node-02",
                "AA:BB:CC:DD:EE:02"
        );
        ipAllocationService.allocateSpecific(
                subnet.getId(),
                "192.168.40.1",
                firstInterface.getId()
        );

        IpAddressStateConflictException exception = assertThrows(
                IpAddressStateConflictException.class,
                () -> ipAllocationService.allocateSpecific(
                        subnet.getId(),
                        "192.168.40.1",
                        secondInterface.getId()
                )
        );

        assertEquals(IpAddressStatus.ALLOCATED, exception.getStatus());
    }

    @Test
    void reportsWhenPoolHasNoAvailableAddress() {
        Subnet subnet = createSubnet("192.168.50.0", 30, "192.168.50.1");
        NetworkInterface networkInterface = createInterface(
                "compute-node-01",
                "AA:BB:CC:DD:EE:01"
        );
        ipAllocationService.allocateNextAvailable(
                subnet.getId(),
                networkInterface.getId()
        );

        assertThrows(
                NoAvailableIpAddressException.class,
                () -> ipAllocationService.allocateNextAvailable(
                        subnet.getId(),
                        networkInterface.getId()
                )
        );
    }

    @Test
    void concurrentRequestsAllocateEachAddressOnlyOnce() throws Exception {
        Subnet subnet = createSubnet("192.168.90.0", 30, null);
        NetworkInterface networkInterface = createInterface(
                "compute-node-01", "AA:BB:CC:DD:EE:01"
        );

        int requests = 6;
        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                Callable<String> task = () -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("Concurrent allocation did not start");
                    }
                    try {
                        return ipAllocationService.allocateNextAvailable(
                                subnet.getId(), networkInterface.getId()
                        ).getAddress();
                    } catch (NoAvailableIpAddressException expected) {
                        return null;
                    }
                };
                futures.add(executor.submit(task));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            List<String> allocated = new ArrayList<>();
            for (Future<String> future : futures) {
                String address = future.get(20, TimeUnit.SECONDS);
                if (address != null) {
                    allocated.add(address);
                }
            }

            assertEquals(2, allocated.size());
            assertEquals(2, allocated.stream().distinct().count());
            assertEquals(2, ipAddressRepository.countBySubnetIdAndStatus(
                    subnet.getId(), IpAddressStatus.ALLOCATED
            ));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void releasesAllocatedAddressAndClearsAssignment() {
        Subnet subnet = createSubnet("192.168.60.0", 30, null);
        NetworkInterface networkInterface = createInterface(
                "compute-node-01",
                "AA:BB:CC:DD:EE:01"
        );
        ipAllocationService.allocateSpecific(
                subnet.getId(),
                "192.168.60.1",
                networkInterface.getId()
        );

        IpAddress released = ipAllocationService.release(
                subnet.getId(),
                "192.168.60.1"
        );
        IpAddress reloaded = ipAddressRepository.findById(released.getId()).orElseThrow();

        assertEquals(IpAddressStatus.AVAILABLE, reloaded.getStatus());
        assertNull(reloaded.getNetworkInterface());
        assertNull(reloaded.getAllocatedAt());
    }

    @Test
    void rejectsReleaseWhenAddressIsNotAllocated() {
        Subnet subnet = createSubnet("192.168.70.0", 30, null);

        IpAddressStateConflictException exception = assertThrows(
                IpAddressStateConflictException.class,
                () -> ipAllocationService.release(
                        subnet.getId(),
                        "192.168.70.1"
                )
        );

        assertEquals(IpAddressStatus.AVAILABLE, exception.getStatus());
    }

    @Test
    void rejectsReleaseOfReservedAddress() {
        Subnet subnet = createSubnet("192.168.75.0", 30, "192.168.75.1");

        IpAddressStateConflictException exception = assertThrows(
                IpAddressStateConflictException.class,
                () -> ipAllocationService.release(
                        subnet.getId(),
                        "192.168.75.1"
                )
        );

        assertEquals(IpAddressStatus.RESERVED, exception.getStatus());
    }

    @Test
    void distinguishesMissingSubnetAddressAndInterface() {
        Subnet subnet = createSubnet("192.168.80.0", 30, null);
        NetworkInterface networkInterface = createInterface(
                "compute-node-01",
                "AA:BB:CC:DD:EE:01"
        );

        assertThrows(
                SubnetNotFoundException.class,
                () -> ipAllocationService.allocateNextAvailable(
                        Long.MAX_VALUE,
                        networkInterface.getId()
                )
        );
        assertThrows(
                NetworkInterfaceNotFoundException.class,
                () -> ipAllocationService.allocateNextAvailable(
                        subnet.getId(),
                        Long.MAX_VALUE
                )
        );
        assertThrows(
                IpAddressNotFoundException.class,
                () -> ipAllocationService.allocateSpecific(
                        subnet.getId(),
                        "192.168.81.1",
                        networkInterface.getId()
                )
        );
    }

    private Subnet createSubnet(
            String networkAddress,
            int prefixLength,
            String gatewayAddress
    ) {
        return subnetPoolPersistenceService.createSubnetWithPool(
                new Subnet(networkAddress, prefixLength, gatewayAddress)
        );
    }

    private NetworkInterface createInterface(String deviceName, String macAddress) {
        Device device = deviceRepository.save(
                new Device(deviceName, DeviceType.SERVER)
        );
        return networkInterfaceRepository.save(
                new NetworkInterface(device, "eth0", macAddress)
        );
    }
}
