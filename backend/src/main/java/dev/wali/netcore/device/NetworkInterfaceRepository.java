package dev.wali.netcore.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface NetworkInterfaceRepository extends JpaRepository<NetworkInterface, Long> {

    List<NetworkInterface> findAllByDeviceIdOrderByNameAsc(Long deviceId);

    @EntityGraph(attributePaths = "device")
    List<NetworkInterface> findAllByOrderByDeviceNameAscNameAsc();
}
