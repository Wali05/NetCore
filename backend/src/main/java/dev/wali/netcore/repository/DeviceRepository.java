package dev.wali.netcore.repository;

import dev.wali.netcore.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByName(String name);

    List<Device> findAllByOrderByNameAsc();
}
