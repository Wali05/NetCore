package dev.wali.netcore.subnet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubnetRepository extends JpaRepository<Subnet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select subnet from Subnet subnet where subnet.id = :id")
    Optional<Subnet> findByIdForAllocation(@Param("id") Long id);

    Optional<Subnet> findByNetworkAddressAndPrefixLength(
            String networkAddress,
            int prefixLength
    );
}
