package dev.wali.netcore.subnet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class IpPoolServiceTests {

    private IpPoolService ipPoolService;

    @BeforeEach
    void setUp() {
        ipPoolService = new IpPoolService();
    }

    @Test
    void producesExactly256IpAddressObjectsForSlash24() {
        Subnet subnet = new Subnet("192.168.1.0", 24, "192.168.1.1");

        List<IpAddress> pool = ipPoolService.generatePool(subnet);

        assertEquals(256, pool.size());
    }

    @Test
    void setsCorrectStatusForSlash24WithConfiguredGateway() {
        Subnet subnet = new Subnet("192.168.1.0", 24, "192.168.1.1");

        List<IpAddress> pool = ipPoolService.generatePool(subnet);

        Map<String, IpAddressStatus> statusByAddress = pool.stream()
                .collect(Collectors.toMap(IpAddress::getAddress, IpAddress::getStatus));

        // Network address is RESERVED
        assertEquals(IpAddressStatus.RESERVED, statusByAddress.get("192.168.1.0"));

        // Configured gateway is RESERVED
        assertEquals(IpAddressStatus.RESERVED, statusByAddress.get("192.168.1.1"));

        // First usable host after gateway is AVAILABLE
        assertEquals(IpAddressStatus.AVAILABLE, statusByAddress.get("192.168.1.2"));

        // Last usable host is AVAILABLE
        assertEquals(IpAddressStatus.AVAILABLE, statusByAddress.get("192.168.1.254"));

        // Broadcast address is RESERVED
        assertEquals(IpAddressStatus.RESERVED, statusByAddress.get("192.168.1.255"));

        // Verify counts: 3 RESERVED (network, gateway, broadcast) and 253 AVAILABLE
        long reservedCount = pool.stream().filter(ip -> ip.getStatus() == IpAddressStatus.RESERVED).count();
        long availableCount = pool.stream().filter(ip -> ip.getStatus() == IpAddressStatus.AVAILABLE).count();

        assertEquals(3, reservedCount);
        assertEquals(253, availableCount);
    }

    @Test
    void subnetWithoutConfiguredGatewayStillReservesNetworkAndBroadcastAddresses() {
        Subnet subnet = new Subnet("10.0.0.0", 24, null);

        List<IpAddress> pool = ipPoolService.generatePool(subnet);

        assertEquals(256, pool.size());

        Map<String, IpAddressStatus> statusByAddress = pool.stream()
                .collect(Collectors.toMap(IpAddress::getAddress, IpAddress::getStatus));

        // Network address is RESERVED
        assertEquals(IpAddressStatus.RESERVED, statusByAddress.get("10.0.0.0"));

        // Broadcast address is RESERVED
        assertEquals(IpAddressStatus.RESERVED, statusByAddress.get("10.0.0.255"));

        // Address 10.0.0.1 is AVAILABLE when no gateway is configured
        assertEquals(IpAddressStatus.AVAILABLE, statusByAddress.get("10.0.0.1"));
        assertEquals(IpAddressStatus.AVAILABLE, statusByAddress.get("10.0.0.254"));

        // Verify counts: exactly 2 RESERVED (network and broadcast) and 254 AVAILABLE
        long reservedCount = pool.stream().filter(ip -> ip.getStatus() == IpAddressStatus.RESERVED).count();
        long availableCount = pool.stream().filter(ip -> ip.getStatus() == IpAddressStatus.AVAILABLE).count();

        assertEquals(2, reservedCount);
        assertEquals(254, availableCount);
    }

    @Test
    void generatesExactlyFourAddressesForSlash30WithCorrectStatuses() {
        Subnet subnet = new Subnet("192.168.1.0", 30, "192.168.1.1");

        List<IpAddress> pool = ipPoolService.generatePool(subnet);

        assertEquals(4, pool.size());

        Map<String, IpAddressStatus> statusByAddress = pool.stream()
                .collect(Collectors.toMap(IpAddress::getAddress, IpAddress::getStatus));

        // Network
        assertEquals(IpAddressStatus.RESERVED, statusByAddress.get("192.168.1.0"));

        // Gateway
        assertEquals(IpAddressStatus.RESERVED, statusByAddress.get("192.168.1.1"));

        // Usable host
        assertEquals(IpAddressStatus.AVAILABLE, statusByAddress.get("192.168.1.2"));

        // Broadcast
        assertEquals(IpAddressStatus.RESERVED, statusByAddress.get("192.168.1.3"));
    }

    @Test
    void generatesExactlyFourAddressesForSlash30WithoutGateway() {
        Subnet subnet = new Subnet("192.168.1.0", 30, null);

        List<IpAddress> pool = ipPoolService.generatePool(subnet);

        assertEquals(4, pool.size());

        Map<String, IpAddressStatus> statusByAddress = pool.stream()
                .collect(Collectors.toMap(IpAddress::getAddress, IpAddress::getStatus));

        assertEquals(IpAddressStatus.RESERVED, statusByAddress.get("192.168.1.0"));
        assertEquals(IpAddressStatus.AVAILABLE, statusByAddress.get("192.168.1.1"));
        assertEquals(IpAddressStatus.AVAILABLE, statusByAddress.get("192.168.1.2"));
        assertEquals(IpAddressStatus.RESERVED, statusByAddress.get("192.168.1.3"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 8, 16, 22, 23, 31, 32})
    void rejectsPrefixLengthsOutsideSupportedRange(int prefixLength) {
        Subnet subnet = new Subnet("10.0.0.0", prefixLength, null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ipPoolService.generatePool(subnet)
        );

        assertTrue(exception.getMessage().contains("Unsupported prefix length"));
    }

    @ParameterizedTest
    @ValueSource(ints = {24, 25, 26, 27, 28, 29, 30})
    void supportsAllPrefixLengthsFrom24Through30(int prefixLength) {
        Subnet subnet = new Subnet("10.0.0.0", prefixLength, null);

        List<IpAddress> pool = ipPoolService.generatePool(subnet);

        int expectedSize = 1 << (32 - prefixLength);
        assertEquals(expectedSize, pool.size());
    }

    @Test
    void rejectsNullSubnet() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ipPoolService.generatePool(null)
        );

        assertEquals("Subnet is required", exception.getMessage());
    }

    @Test
    void verifiesGeneratedNumericAddressesMatchTextualAddresses() {
        Subnet subnet = new Subnet("172.16.5.0", 28, "172.16.5.1");

        List<IpAddress> pool = ipPoolService.generatePool(subnet);

        assertEquals(16, pool.size());

        for (IpAddress ipAddress : pool) {
            long expectedNumeric = Ipv4AddressCalculator.toNumeric(ipAddress.getAddress());
            String expectedTextual = Ipv4AddressCalculator.toAddress(ipAddress.getAddressNumeric());

            assertEquals(expectedNumeric, ipAddress.getAddressNumeric());
            assertEquals(expectedTextual, ipAddress.getAddress());
            assertSame(subnet, ipAddress.getSubnet());
        }
    }

    @Test
    void cannotAllocateReservedIpAddress() {
        Subnet subnet = new Subnet("192.168.1.0", 24, "192.168.1.1");
        IpAddress reservedIp = IpAddress.createReserved(subnet, "192.168.1.0");

        assertEquals(IpAddressStatus.RESERVED, reservedIp.getStatus());
        assertThrows(
                IllegalStateException.class,
                () -> reservedIp.allocateTo(null)
        );
    }
}
