package dev.wali.netcore.repository;

import dev.wali.netcore.domain.NetworkInterface;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NetworkInterfaceRepository extends JpaRepository<NetworkInterface, Long> {

    List<NetworkInterface> findAllByDeviceIdOrderByNameAsc(Long deviceId);
}
