package dev.wali.netcore.exception;

public class IpAddressNotFoundException extends RuntimeException {

    public IpAddressNotFoundException(Long subnetId, String address) {
        super("IP address " + address + " was not found in subnet " + subnetId);
    }
}
