package dev.wali.netcore.service;

import dev.wali.netcore.domain.IpAddress;
import dev.wali.netcore.domain.Ipv4AddressCalculator;
import dev.wali.netcore.domain.Subnet;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IpPoolService {

    private static final int MIN_PREFIX_LENGTH = 24;
    private static final int MAX_PREFIX_LENGTH = 30;

    public List<IpAddress> generatePool(Subnet subnet) {
        if (subnet == null) {
            throw new IllegalArgumentException("Subnet is required");
        }

        int prefixLength = subnet.getPrefixLength();
        if (prefixLength < MIN_PREFIX_LENGTH || prefixLength > MAX_PREFIX_LENGTH) {
            throw new IllegalArgumentException(
                    "Unsupported prefix length: /" + prefixLength
                            + ". Pool materialization supports prefix lengths between /"
                            + MIN_PREFIX_LENGTH + " and /" + MAX_PREFIX_LENGTH + " only."
            );
        }

        long networkNumeric = Ipv4AddressCalculator.toNumeric(subnet.getNetworkAddress());
        long broadcastNumeric = Ipv4AddressCalculator.broadcastAddress(networkNumeric, prefixLength);

        Long gatewayNumeric = subnet.getGatewayAddress() != null
                ? Ipv4AddressCalculator.toNumeric(subnet.getGatewayAddress())
                : null;

        List<IpAddress> addresses = new ArrayList<>();

        for (long currentNumeric = networkNumeric; currentNumeric <= broadcastNumeric; currentNumeric++) {
            String currentAddress = Ipv4AddressCalculator.toAddress(currentNumeric);

            boolean isReserved = (currentNumeric == networkNumeric)
                    || (currentNumeric == broadcastNumeric)
                    || (gatewayNumeric != null && currentNumeric == gatewayNumeric.longValue());

            if (isReserved) {
                addresses.add(IpAddress.createReserved(subnet, currentAddress));
            } else {
                addresses.add(IpAddress.createAvailable(subnet, currentAddress));
            }
        }

        return addresses;
    }

    public List<IpAddress> generateAddresses(Subnet subnet) {
        return generatePool(subnet);
    }
}
