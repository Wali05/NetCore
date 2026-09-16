package dev.wali.netcore.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByName(String name);

    List<Device> findAllByOrderByNameAsc();
}
