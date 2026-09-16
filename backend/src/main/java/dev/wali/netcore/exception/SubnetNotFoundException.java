package dev.wali.netcore.exception;

public class SubnetNotFoundException extends RuntimeException {

    public SubnetNotFoundException(Long subnetId) {
        super("Subnet not found: " + subnetId);
    }
}
