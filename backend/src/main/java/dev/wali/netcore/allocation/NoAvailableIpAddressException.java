package dev.wali.netcore.allocation;

public class NoAvailableIpAddressException extends RuntimeException {

    public NoAvailableIpAddressException(Long subnetId) {
        super("No available IP address remains in subnet " + subnetId);
    }
}
