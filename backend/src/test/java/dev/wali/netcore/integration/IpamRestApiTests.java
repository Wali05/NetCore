package dev.wali.netcore.integration;

import dev.wali.netcore.device.Device;
import dev.wali.netcore.device.DeviceRepository;
import dev.wali.netcore.device.NetworkInterfaceRepository;
import dev.wali.netcore.subnet.IpAddressRepository;
import dev.wali.netcore.subnet.Subnet;
import dev.wali.netcore.subnet.SubnetRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class IpamRestApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IpAddressRepository ipAddressRepository;

    @Autowired
    private NetworkInterfaceRepository networkInterfaceRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SubnetRepository subnetRepository;

    @BeforeEach
    void clearDatabase() {
        ipAddressRepository.deleteAll();
        networkInterfaceRepository.deleteAll();
        deviceRepository.deleteAll();
        subnetRepository.deleteAll();
    }

    @AfterEach
    void cleanUpDatabase() {
        clearDatabase();
    }

    @Test
    void createsSubnetAndListsItsAddressPool() throws Exception {
        createSubnet();
        Subnet subnet = savedSubnet();

        mockMvc.perform(get("/api/v1/subnets/{subnetId}/addresses", subnet.getId())
                        .param("status", "RESERVED")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].address").value("10.20.30.8"))
                .andExpect(jsonPath("$.content[0].status").value("RESERVED"))
                .andExpect(jsonPath("$.content[1].address").value("10.20.30.9"));
    }

    @Test
    void createsDeviceAndInterface() throws Exception {
        createDevice();
        Device device = savedDevice();

        mockMvc.perform(post("/api/v1/devices/{deviceId}/interfaces", device.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "eth0",
                                  "macAddress": "aa:bb:cc:dd:ee:01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value(device.getId()))
                .andExpect(jsonPath("$.deviceName").value("edge-router-01"))
                .andExpect(jsonPath("$.name").value("eth0"))
                .andExpect(jsonPath("$.macAddress").value("AA:BB:CC:DD:EE:01"))
                .andExpect(jsonPath("$.assignedAddresses").isEmpty());

        mockMvc.perform(get("/api/v1/devices/{deviceId}/interfaces", device.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("eth0"));
    }

    @Test
    void allocatesNextAddressShowsAssignmentAndReleasesIt() throws Exception {
        createSubnet();
        createDevice();
        Subnet subnet = savedSubnet();
        Device device = savedDevice();

        mockMvc.perform(post("/api/v1/devices/{deviceId}/interfaces", device.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "eth0",
                                  "macAddress": "AA:BB:CC:DD:EE:02"
                                }
                                """))
                .andExpect(status().isCreated());

        Long interfaceId = networkInterfaceRepository
                .findAllByDeviceIdOrderByNameAsc(device.getId())
                .getFirst()
                .getId();

        mockMvc.perform(post("/api/v1/subnets/{subnetId}/allocate-next", subnet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"networkInterfaceId\":" + interfaceId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("10.20.30.10"))
                .andExpect(jsonPath("$.status").value("ALLOCATED"))
                .andExpect(jsonPath("$.networkInterfaceId").value(interfaceId))
                .andExpect(jsonPath("$.deviceName").value("edge-router-01"))
                .andExpect(jsonPath("$.allocatedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/interfaces/{interfaceId}", interfaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAddresses[0].address")
                        .value("10.20.30.10"));

        mockMvc.perform(post(
                        "/api/v1/subnets/{subnetId}/addresses/{address}/release",
                        subnet.getId(),
                        "10.20.30.10"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.networkInterfaceId").doesNotExist())
                .andExpect(jsonPath("$.allocatedAt").doesNotExist());

        mockMvc.perform(get("/api/v1/interfaces/{interfaceId}", interfaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAddresses").isEmpty());
    }

    @Test
    void returnsStructuredValidationNotFoundAndConflictErrors() throws Exception {
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"ROUTER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/devices"));

        mockMvc.perform(get("/api/v1/subnets/{subnetId}/addresses", 999999)
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/subnets/{subnetId}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBNET_NOT_FOUND"));

        createSubnet();
        createDevice();
        Subnet subnet = savedSubnet();
        Device device = savedDevice();

        mockMvc.perform(post("/api/v1/devices/{deviceId}/interfaces", device.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "eth0",
                                  "macAddress": "AA:BB:CC:DD:EE:03"
                                }
                                """))
                .andExpect(status().isCreated());

        Long interfaceId = networkInterfaceRepository
                .findAllByDeviceIdOrderByNameAsc(device.getId())
                .getFirst()
                .getId();

        mockMvc.perform(post(
                                "/api/v1/subnets/{subnetId}/addresses/{address}/allocate",
                                subnet.getId(),
                                "10.20.30.8"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"networkInterfaceId\":" + interfaceId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IP_RESERVED"));
    }

    private void createSubnet() throws Exception {
        mockMvc.perform(post("/api/v1/subnets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "networkAddress": "10.20.30.12",
                                  "prefixLength": 29,
                                  "gatewayAddress": "10.20.30.9"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.networkAddress").value("10.20.30.8"))
                .andExpect(jsonPath("$.cidr").value("10.20.30.8/29"))
                .andExpect(jsonPath("$.totalAddresses").value(8))
                .andExpect(jsonPath("$.availableAddresses").value(5))
                .andExpect(jsonPath("$.reservedAddresses").value(3));
    }

    private void createDevice() throws Exception {
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "edge-router-01",
                                  "type": "ROUTER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("edge-router-01"))
                .andExpect(jsonPath("$.type").value("ROUTER"));
    }

    private Subnet savedSubnet() {
        return subnetRepository
                .findByNetworkAddressAndPrefixLength("10.20.30.8", 29)
                .orElseThrow();
    }

    private Device savedDevice() {
        return deviceRepository.findByName("edge-router-01").orElseThrow();
    }
}
