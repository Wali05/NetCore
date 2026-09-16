package dev.wali.netcore.repository;

import dev.wali.netcore.domain.IpAddress;
import dev.wali.netcore.domain.IpAddressStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IpAddressRepository extends JpaRepository<IpAddress, Long> {

    List<IpAddress> findAllBySubnetIdOrderByAddressNumericAsc(Long subnetId);

    Page<IpAddress> findAllBySubnetId(Long subnetId, Pageable pageable);

    Page<IpAddress> findAllBySubnetIdAndStatus(
            Long subnetId,
            IpAddressStatus status,
            Pageable pageable
    );

    Optional<IpAddress> findBySubnetIdAndAddress(Long subnetId, String address);

    Optional<IpAddress> findFirstBySubnetIdAndStatusOrderByAddressNumericAsc(
            Long subnetId,
            IpAddressStatus status
    );

    long countBySubnetIdAndStatus(Long subnetId, IpAddressStatus status);

    @EntityGraph(attributePaths = {
            "subnet",
            "networkInterface",
            "networkInterface.device"
    })
    @Query("select ip from IpAddress ip where ip.id = :id")
    Optional<IpAddress> findWithAssignmentById(@Param("id") Long id);
}
