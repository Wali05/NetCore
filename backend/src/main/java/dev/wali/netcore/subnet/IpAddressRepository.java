package dev.wali.netcore.subnet;

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

    @EntityGraph(attributePaths = {"networkInterface", "networkInterface.device"})
    Page<IpAddress> findAllBySubnetId(Long subnetId, Pageable pageable);

    @EntityGraph(attributePaths = {"networkInterface", "networkInterface.device"})
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

    @Query("select ip.subnet.id as subnetId, ip.status as status, count(ip) as total "
            + "from IpAddress ip group by ip.subnet.id, ip.status")
    List<AddressStatusCount> countAllBySubnetAndStatus();

    @Query("select ip.subnet.id as subnetId, ip.status as status, count(ip) as total "
            + "from IpAddress ip where ip.subnet.id = :subnetId group by ip.subnet.id, ip.status")
    List<AddressStatusCount> countBySubnetAndStatus(@Param("subnetId") Long subnetId);

    @EntityGraph(attributePaths = {"networkInterface", "subnet"})
    List<IpAddress> findAllByNetworkInterfaceIsNotNullOrderByAddressNumericAsc();

    List<IpAddress> findAllByNetworkInterfaceIdOrderByAddressNumericAsc(
            Long networkInterfaceId
    );

    @EntityGraph(attributePaths = {
            "subnet",
            "networkInterface",
            "networkInterface.device"
    })
    @Query("select ip from IpAddress ip where ip.id = :id")
    Optional<IpAddress> findWithAssignmentById(@Param("id") Long id);
}
