package dev.wali.netcore.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceModelTests {

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
