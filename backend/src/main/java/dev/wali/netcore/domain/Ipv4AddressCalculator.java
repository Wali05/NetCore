package dev.wali.netcore.domain;

public final class Ipv4AddressCalculator {

    private static final long MAX_IPV4 = 0xFFFF_FFFFL;

    private Ipv4AddressCalculator() {
    }

    public static long toNumeric(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("IPv4 address is required");
        }

        String[] parts = address.split("\\.", -1);

        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + address);
        }

        long result = 0;

        for (String part : parts) {
            if (!part.matches("[0-9]{1,3}")) {
                throw new IllegalArgumentException("Invalid IPv4 address: " + address);
            }

            int octet;

            try {
                octet = Integer.parseInt(part);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Invalid IPv4 address: " + address,
                        exception
                );
            }

            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Invalid IPv4 address: " + address);
            }

            result = (result << 8) | octet;
        }

        return result;
    }

    public static String toAddress(long numericAddress) {
        validateNumericAddress(numericAddress);

        return "%d.%d.%d.%d".formatted(
                (numericAddress >> 24) & 255,
                (numericAddress >> 16) & 255,
                (numericAddress >> 8) & 255,
                numericAddress & 255
        );
    }

    public static long networkAddress(long address, int prefixLength) {
        validateNumericAddress(address);
        validatePrefix(prefixLength);

        long mask = prefixLength == 0
                ? 0
                : (MAX_IPV4 << (32 - prefixLength)) & MAX_IPV4;

        return address & mask;
    }

    public static long broadcastAddress(long address, int prefixLength) {
        long network = networkAddress(address, prefixLength);

        long hostMask = prefixLength == 32
                ? 0
                : (1L << (32 - prefixLength)) - 1;

        return network | hostMask;
    }

    public static boolean belongsToSubnet(
            String address,
            String networkAddress,
            int prefixLength
    ) {
        long ip = toNumeric(address);
        long network = toNumeric(networkAddress);

        return networkAddress(ip, prefixLength)
                == networkAddress(network, prefixLength);
    }

    private static void validatePrefix(int prefixLength) {
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException(
                    "IPv4 prefix length must be between 0 and 32"
            );
        }
    }

    private static void validateNumericAddress(long address) {
        if (address < 0 || address > MAX_IPV4) {
            throw new IllegalArgumentException(
                    "IPv4 numeric value must be between 0 and " + MAX_IPV4
            );
        }
    }
}
