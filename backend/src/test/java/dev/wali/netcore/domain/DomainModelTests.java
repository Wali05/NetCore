package dev.wali.netcore.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainModelTests {

    @Test
    void canonicalizesSubnetAddress() {
        Subnet subnet = new Subnet("192.168.1.25", 24, "192.168.1.1");

        assertEquals("192.168.1.0", subnet.getNetworkAddress());
        assertEquals("192.168.1.1", subnet.getGatewayAddress());
    }

    @Test
    void rejectsGatewayOutsideSubnet() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Subnet("192.168.1.0", 24, "192.168.2.1")
        );
    }

    @Test
    void createsAvailableIpAddressWithDerivedNumericValue() {
        Subnet subnet = new Subnet("192.168.1.0", 24, "192.168.1.1");

        IpAddress ipAddress = new IpAddress(subnet, "192.168.1.25");

        assertEquals("192.168.1.25", ipAddress.getAddress());
        assertEquals(3232235801L, ipAddress.getAddressNumeric());
        assertEquals(IpAddressStatus.AVAILABLE, ipAddress.getStatus());
    }

    @Test
    void rejectsIpAddressOutsideSubnet() {
        Subnet subnet = new Subnet("192.168.1.0", 24, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> new IpAddress(subnet, "192.168.2.25")
        );
    }

    @Test
    void validatesAndNormalizesNetworkInterface() {
        Device device = new Device("server-01", DeviceType.SERVER);

        NetworkInterface networkInterface = new NetworkInterface(
                device,
                "eth0",
                "aa:bb:cc:dd:ee:ff"
        );

        assertEquals("AA:BB:CC:DD:EE:FF", networkInterface.getMacAddress());
        assertThrows(
                IllegalArgumentException.class,
                () -> new NetworkInterface(device, "eth1", "invalid")
        );
    }

    @Test
    void rejectsDeviceWithoutRequiredFields() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Device(" ", DeviceType.SERVER)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new Device("server-01", null)
        );
    }
}
