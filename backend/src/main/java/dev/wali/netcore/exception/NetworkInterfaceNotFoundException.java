package dev.wali.netcore.exception;

public class NetworkInterfaceNotFoundException extends RuntimeException {

    public NetworkInterfaceNotFoundException(Long networkInterfaceId) {
        super("Network interface not found: " + networkInterfaceId);
    }
}
