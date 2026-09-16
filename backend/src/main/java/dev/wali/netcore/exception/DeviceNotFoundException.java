package dev.wali.netcore.exception;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(Long deviceId) {
        super("Device not found: " + deviceId);
    }
}
