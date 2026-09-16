package dev.wali.netcore.service;

import dev.wali.netcore.domain.Device;
import dev.wali.netcore.domain.DeviceType;
import dev.wali.netcore.domain.IpAddress;
import dev.wali.netcore.domain.NetworkInterface;
import dev.wali.netcore.dto.DeviceResponse;
import dev.wali.netcore.dto.IpAssignmentResponse;
import dev.wali.netcore.dto.NetworkInterfaceResponse;
import dev.wali.netcore.exception.DeviceNotFoundException;
import dev.wali.netcore.exception.NetworkInterfaceNotFoundException;
import dev.wali.netcore.repository.DeviceRepository;
import dev.wali.netcore.repository.IpAddressRepository;
import dev.wali.netcore.repository.NetworkInterfaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeviceManagementService {

    private final DeviceRepository deviceRepository;
    private final NetworkInterfaceRepository networkInterfaceRepository;
    private final IpAddressRepository ipAddressRepository;

    public DeviceManagementService(
            DeviceRepository deviceRepository,
            NetworkInterfaceRepository networkInterfaceRepository,
            IpAddressRepository ipAddressRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.networkInterfaceRepository = networkInterfaceRepository;
        this.ipAddressRepository = ipAddressRepository;
    }

    @Transactional
    public DeviceResponse createDevice(String name, DeviceType type) {
        return toDeviceResponse(deviceRepository.save(new Device(name, type)));
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> listDevices() {
        return deviceRepository.findAllByOrderByNameAsc().stream()
                .map(this::toDeviceResponse)
                .toList();
    }

    @Transactional
    public NetworkInterfaceResponse createInterface(
            Long deviceId,
            String name,
            String macAddress
    ) {
        Device device = requireDevice(deviceId);
        NetworkInterface networkInterface = networkInterfaceRepository.save(
                new NetworkInterface(device, name, macAddress)
        );

        return toInterfaceResponse(networkInterface, List.of());
    }

    @Transactional(readOnly = true)
    public List<NetworkInterfaceResponse> listInterfaces(Long deviceId) {
        requireDevice(deviceId);

        return networkInterfaceRepository.findAllByDeviceIdOrderByNameAsc(deviceId).stream()
                .map(this::toInterfaceResponseWithAssignments)
                .toList();
    }

    @Transactional(readOnly = true)
    public NetworkInterfaceResponse getInterface(Long networkInterfaceId) {
        NetworkInterface networkInterface = networkInterfaceRepository
                .findById(networkInterfaceId)
                .orElseThrow(() -> new NetworkInterfaceNotFoundException(networkInterfaceId));

        return toInterfaceResponseWithAssignments(networkInterface);
    }

    private Device requireDevice(Long deviceId) {
        if (deviceId == null) {
            throw new DeviceNotFoundException(null);
        }

        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    private DeviceResponse toDeviceResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getType(),
                device.getCreatedAt()
        );
    }

    private NetworkInterfaceResponse toInterfaceResponseWithAssignments(
            NetworkInterface networkInterface
    ) {
        List<IpAddress> assignments = ipAddressRepository
                .findAllByNetworkInterfaceIdOrderByAddressNumericAsc(networkInterface.getId());
        return toInterfaceResponse(networkInterface, assignments);
    }

    private NetworkInterfaceResponse toInterfaceResponse(
            NetworkInterface networkInterface,
            List<IpAddress> assignments
    ) {
        return new NetworkInterfaceResponse(
                networkInterface.getId(),
                networkInterface.getDevice().getId(),
                networkInterface.getDevice().getName(),
                networkInterface.getName(),
                networkInterface.getMacAddress(),
                networkInterface.getCreatedAt(),
                assignments.stream().map(this::toAssignmentResponse).toList()
        );
    }

    private IpAssignmentResponse toAssignmentResponse(IpAddress ipAddress) {
        return new IpAssignmentResponse(
                ipAddress.getId(),
                ipAddress.getSubnet().getId(),
                ipAddress.getAddress(),
                ipAddress.getAllocatedAt()
        );
    }
}
