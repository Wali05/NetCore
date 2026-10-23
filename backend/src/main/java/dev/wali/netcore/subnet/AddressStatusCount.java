package dev.wali.netcore.subnet;

/** A database aggregate, without loading individual address rows. */
public interface AddressStatusCount {
    Long getSubnetId();
    IpAddressStatus getStatus();
    long getTotal();
}
