package dev.wali.netcore.integration;

import dev.wali.netcore.device.*;
import dev.wali.netcore.subnet.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@ActiveProfiles("local")
class InventoryReadTests {
    @Autowired SubnetManagementService subnets;
    @Autowired DeviceManagementService devices;
    @Autowired IpAddressRepository addresses;
    @Autowired NetworkInterfaceRepository interfaces;
    @Autowired DeviceRepository deviceRepository;
    @Autowired SubnetRepository subnetRepository;
    @Autowired EntityManagerFactory entityManagerFactory;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        addresses.deleteAll();
        interfaces.deleteAll();
        deviceRepository.deleteAll();
        subnetRepository.deleteAll();
    }

    @Test
    void batchesCountsAndSortsSubnetsNumerically() {
        subnets.createSubnet("10.0.10.0", 29, null);
        var subnet = subnets.createSubnet("10.0.2.0", 29, "10.0.2.1");
        var device = devices.createDevice("test-server", DeviceType.SERVER);
        var networkInterface = devices.createInterface(device.id(), "eth0", "02:00:00:00:00:01");
        subnets.allocateNextAvailable(subnet.id(), networkInterface.id());

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result = subnets.listSubnets();

        assertEquals(2, statistics.getPrepareStatementCount(), "One grouped count and one subnet query");
        assertEquals(2, result.size());
        assertEquals("10.0.2.0/29", result.getFirst().cidr());
        assertEquals(4, result.getFirst().availableAddresses());
        assertEquals(1, result.getFirst().allocatedAddresses());
        assertEquals(3, result.getFirst().reservedAddresses());
        assertEquals(20.0, result.getFirst().utilizationPercent());
        assertEquals(6, result.getLast().availableAddresses());
        assertEquals(0, result.getLast().allocatedAddresses());
    }

    @Test
    void listsAllInterfacesAndAssignmentsWithoutPerDeviceQueries() {
        var subnet = subnets.createSubnet("10.1.0.0", 29, null);
        for (int i = 0; i < 4; i++) {
            var device = devices.createDevice("server-" + i, DeviceType.SERVER);
            var networkInterface = devices.createInterface(device.id(), "eth0", "02:00:00:00:01:0" + i);
            if (i < 3) subnets.allocateNextAvailable(subnet.id(), networkInterface.id());
        }

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result = devices.listAllInterfaces();

        assertEquals(2, statistics.getPrepareStatementCount(), "Interfaces with devices, then assignments");
        assertEquals(4, result.size());
        assertEquals("server-0", result.getFirst().deviceName());
        assertEquals("10.1.0.1", result.getFirst().assignedAddresses().getFirst().address());
        assertTrue(result.getLast().assignedAddresses().isEmpty());
        subnets.release(subnet.id(), "10.1.0.1");
        assertTrue(devices.listAllInterfaces().getFirst().assignedAddresses().isEmpty());
    }

    @Test
    void returnsEmptyInventoryWithoutErrors() {
        assertTrue(subnets.listSubnets().isEmpty());
        assertTrue(devices.listAllInterfaces().isEmpty());
    }
}

