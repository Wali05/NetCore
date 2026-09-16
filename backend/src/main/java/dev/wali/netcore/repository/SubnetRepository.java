package dev.wali.netcore.repository;

import dev.wali.netcore.domain.Subnet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubnetRepository extends JpaRepository<Subnet, Long> {

    Optional<Subnet> findByNetworkAddressAndPrefixLength(
            String networkAddress,
            int prefixLength
    );
}
