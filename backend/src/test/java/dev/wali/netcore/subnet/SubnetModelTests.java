package dev.wali.netcore.subnet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubnetModelTests {

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
    void rejectsNetworkOrBroadcastAddressAsGateway() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Subnet("192.168.1.0", 24, "192.168.1.0")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new Subnet("192.168.1.0", 24, "192.168.1.255")
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

}
