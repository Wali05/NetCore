package dev.wali.netcore.device;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceManagementService deviceManagementService;

    public DeviceController(DeviceManagementService deviceManagementService) {
        this.deviceManagementService = deviceManagementService;
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(
            @Valid @RequestBody CreateDeviceRequest request
    ) {
        DeviceResponse response = deviceManagementService.createDevice(
                request.name(),
                request.type()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<DeviceResponse> listDevices() {
        return deviceManagementService.listDevices();
    }

    @PostMapping("/{deviceId}/interfaces")
    public ResponseEntity<NetworkInterfaceResponse> createInterface(
            @PathVariable Long deviceId,
            @Valid @RequestBody CreateNetworkInterfaceRequest request
    ) {
        NetworkInterfaceResponse response = deviceManagementService.createInterface(
                deviceId,
                request.name(),
                request.macAddress()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{deviceId}/interfaces")
    public List<NetworkInterfaceResponse> listInterfaces(@PathVariable Long deviceId) {
        return deviceManagementService.listInterfaces(deviceId);
    }
}
