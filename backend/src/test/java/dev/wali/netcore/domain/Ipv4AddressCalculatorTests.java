package dev.wali.netcore.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Ipv4AddressCalculatorTests {

    @Test
    void convertsIpv4AddressToNumericAndBack() {
        long numeric = Ipv4AddressCalculator.toNumeric("192.168.1.25");

        assertEquals(3232235801L, numeric);
        assertEquals(
                "192.168.1.25",
                Ipv4AddressCalculator.toAddress(numeric)
        );
    }

    @Test
    void calculatesNetworkAndBroadcastForSlash24() {
        long address = Ipv4AddressCalculator.toNumeric("192.168.1.25");

        long network =
                Ipv4AddressCalculator.networkAddress(address, 24);

        long broadcast =
                Ipv4AddressCalculator.broadcastAddress(address, 24);

        assertEquals(
                "192.168.1.0",
                Ipv4AddressCalculator.toAddress(network)
        );

        assertEquals(
                "192.168.1.255",
                Ipv4AddressCalculator.toAddress(broadcast)
        );
    }

    @Test
    void detectsWhetherAddressBelongsToSubnet() {
        assertTrue(
                Ipv4AddressCalculator.belongsToSubnet(
                        "192.168.1.25",
                        "192.168.1.0",
                        24
                )
        );

        assertFalse(
                Ipv4AddressCalculator.belongsToSubnet(
                        "192.168.2.25",
                        "192.168.1.0",
                        24
                )
        );
    }

    @Test
    void rejectsInvalidIpv4Address() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Ipv4AddressCalculator.toNumeric("192.168.1.999")
        );
    }

    @Test
    void rejectsInvalidPrefixLength() {
        long address = Ipv4AddressCalculator.toNumeric("192.168.1.25");

        assertThrows(
                IllegalArgumentException.class,
                () -> Ipv4AddressCalculator.networkAddress(address, 33)
        );
    }

    @Test
    void rejectsNumericAddressOutsideIpv4Range() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Ipv4AddressCalculator.networkAddress(-1, 24)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Ipv4AddressCalculator.toAddress(0x1_0000_0000L)
        );
    }
}
