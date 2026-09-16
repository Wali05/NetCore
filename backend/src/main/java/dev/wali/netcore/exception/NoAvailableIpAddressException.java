package dev.wali.netcore.exception;

public class NoAvailableIpAddressException extends RuntimeException {

    public NoAvailableIpAddressException(Long subnetId) {
        super("No available IP address remains in subnet " + subnetId);
    }
}
