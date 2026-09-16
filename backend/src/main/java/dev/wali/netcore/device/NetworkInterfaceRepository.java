package dev.wali.netcore.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NetworkInterfaceRepository extends JpaRepository<NetworkInterface, Long> {

    List<NetworkInterface> findAllByDeviceIdOrderByNameAsc(Long deviceId);
}
