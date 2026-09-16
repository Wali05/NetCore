package dev.wali.netcore.controller;

import dev.wali.netcore.dto.NetworkInterfaceResponse;
import dev.wali.netcore.service.DeviceManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interfaces")
public class NetworkInterfaceController {

    private final DeviceManagementService deviceManagementService;

    public NetworkInterfaceController(DeviceManagementService deviceManagementService) {
        this.deviceManagementService = deviceManagementService;
    }

    @GetMapping("/{networkInterfaceId}")
    public NetworkInterfaceResponse getInterface(@PathVariable Long networkInterfaceId) {
        return deviceManagementService.getInterface(networkInterfaceId);
    }
}
