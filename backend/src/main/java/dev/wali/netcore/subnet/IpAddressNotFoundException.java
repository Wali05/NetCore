package dev.wali.netcore.subnet;

public class IpAddressNotFoundException extends RuntimeException {

    public IpAddressNotFoundException(Long subnetId, String address) {
        super("IP address " + address + " was not found in subnet " + subnetId);
    }
}
