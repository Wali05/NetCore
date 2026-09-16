package dev.wali.netcore.device;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(Long deviceId) {
        super("Device not found: " + deviceId);
    }
}
