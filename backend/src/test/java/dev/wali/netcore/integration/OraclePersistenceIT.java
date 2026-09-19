package dev.wali.netcore.integration;

import dev.wali.netcore.allocation.IpAllocationService;
import dev.wali.netcore.allocation.NoAvailableIpAddressException;
import dev.wali.netcore.device.Device;
import dev.wali.netcore.device.DeviceRepository;
import dev.wali.netcore.device.DeviceType;
import dev.wali.netcore.device.NetworkInterface;
import dev.wali.netcore.device.NetworkInterfaceRepository;
import dev.wali.netcore.subnet.IpAddress;
import dev.wali.netcore.subnet.IpAddressRepository;
import dev.wali.netcore.subnet.IpAddressStatus;
import dev.wali.netcore.subnet.Subnet;
import dev.wali.netcore.subnet.SubnetPoolPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@ActiveProfiles("oracle")
class OraclePersistenceIT {

    @Container
    static final OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @DynamicPropertySource
    static void oracleProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", oracle::getJdbcUrl);
        registry.add("spring.datasource.username", oracle::getUsername);
        registry.add("spring.datasource.password", oracle::getPassword);
        // This is a throwaway schema. The runtime Oracle profile still validates an existing schema.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SubnetPoolPersistenceService subnetPoolPersistenceService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private NetworkInterfaceRepository networkInterfaceRepository;

    @Autowired
    private IpAddressRepository ipAddressRepository;

    @Autowired
    private IpAllocationService ipAllocationService;

    @Test
    void persistsAndAllocatesOnOracleUnderContention() throws Exception {
        String databaseName = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                connection.getMetaData().getDatabaseProductName());
        assertTrue(databaseName.contains("Oracle"));

        Subnet subnet = subnetPoolPersistenceService.createSubnetWithPool(
                new Subnet("198.51.100.0", 30, null)
        );
        NetworkInterface networkInterface = networkInterfaceRepository.save(
                new NetworkInterface(
                        deviceRepository.save(new Device("oracle-node", DeviceType.SERVER)),
                        "eth0", "AA:BB:CC:DD:EE:90"
                )
        );

        assertEquals(4, ipAddressRepository.count());
        assertEquals(2, ipAddressRepository.countBySubnetIdAndStatus(
                subnet.getId(), IpAddressStatus.AVAILABLE));

        int requests = 6;
        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                Callable<String> task = () -> {
                    ready.countDown();
                    if (!start.await(20, TimeUnit.SECONDS)) {
                        throw new AssertionError("Concurrent allocation did not start");
                    }
                    try {
                        return ipAllocationService.allocateNextAvailable(
                                subnet.getId(), networkInterface.getId()).getAddress();
                    } catch (NoAvailableIpAddressException expected) {
                        return null;
                    }
                };
                futures.add(executor.submit(task));
            }

            assertTrue(ready.await(20, TimeUnit.SECONDS));
            start.countDown();

            List<String> allocated = new ArrayList<>();
            for (Future<String> future : futures) {
                String address = future.get(60, TimeUnit.SECONDS);
                if (address != null) {
                    allocated.add(address);
                }
            }

            assertEquals(2, allocated.size());
            assertEquals(2, allocated.stream().distinct().count());
            assertEquals(2, ipAddressRepository.countBySubnetIdAndStatus(
                    subnet.getId(), IpAddressStatus.ALLOCATED));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        IpAddress assigned = ipAddressRepository.findBySubnetIdAndAddress(
                subnet.getId(), "198.51.100.1").orElseThrow();
        assertNotNull(assigned.getNetworkInterface());
        assertNotNull(assigned.getAllocatedAt());

        ipAllocationService.release(subnet.getId(), assigned.getAddress());
        IpAddress released = ipAddressRepository.findById(assigned.getId()).orElseThrow();
        assertEquals(IpAddressStatus.AVAILABLE, released.getStatus());
        assertNull(released.getNetworkInterface());
        assertNull(released.getAllocatedAt());
    }
}
