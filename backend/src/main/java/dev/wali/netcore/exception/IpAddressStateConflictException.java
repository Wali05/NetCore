package dev.wali.netcore.exception;

import dev.wali.netcore.domain.IpAddressStatus;

public class IpAddressStateConflictException extends RuntimeException {

    private final String address;
    private final IpAddressStatus status;

    public IpAddressStateConflictException(
            String address,
            IpAddressStatus status,
            String requestedOperation
    ) {
        super("IP address " + address + " cannot be " + requestedOperation
                + " while its status is " + status);
        this.address = address;
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public IpAddressStatus getStatus() {
        return status;
    }
}
