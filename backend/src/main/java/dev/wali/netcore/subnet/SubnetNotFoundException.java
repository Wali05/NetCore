package dev.wali.netcore.subnet;

public class SubnetNotFoundException extends RuntimeException {

    public SubnetNotFoundException(Long subnetId) {
        super("Subnet not found: " + subnetId);
    }
}
