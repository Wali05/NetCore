package dev.wali.netcore.subnet;

import dev.wali.netcore.allocation.AllocationRequest;
import dev.wali.netcore.shared.PageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/subnets")
public class SubnetController {

    private final SubnetManagementService subnetManagementService;

    public SubnetController(SubnetManagementService subnetManagementService) {
        this.subnetManagementService = subnetManagementService;
    }

    @PostMapping
    public ResponseEntity<SubnetResponse> createSubnet(
            @Valid @RequestBody CreateSubnetRequest request
    ) {
        SubnetResponse response = subnetManagementService.createSubnet(
                request.networkAddress(),
                request.prefixLength(),
                request.gatewayAddress()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<SubnetResponse> listSubnets() {
        return subnetManagementService.listSubnets();
    }

    @GetMapping("/{subnetId}")
    public SubnetResponse getSubnet(@PathVariable Long subnetId) {
        return subnetManagementService.getSubnet(subnetId);
    }

    @GetMapping("/{subnetId}/addresses")
    public PageResponse<IpAddressResponse> listAddresses(
            @PathVariable Long subnetId,
            @RequestParam(required = false) IpAddressStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size
    ) {
        return subnetManagementService.listAddresses(subnetId, status, page, size);
    }

    @PostMapping("/{subnetId}/addresses/{address}/allocate")
    public IpAddressResponse allocateSpecific(
            @PathVariable Long subnetId,
            @PathVariable String address,
            @Valid @RequestBody AllocationRequest request
    ) {
        return subnetManagementService.allocateSpecific(
                subnetId,
                address,
                request.networkInterfaceId()
        );
    }

    @PostMapping("/{subnetId}/allocate-next")
    public IpAddressResponse allocateNext(
            @PathVariable Long subnetId,
            @Valid @RequestBody AllocationRequest request
    ) {
        return subnetManagementService.allocateNextAvailable(
                subnetId,
                request.networkInterfaceId()
        );
    }

    @PostMapping("/{subnetId}/addresses/{address}/release")
    public IpAddressResponse release(
            @PathVariable Long subnetId,
            @PathVariable String address
    ) {
        return subnetManagementService.release(subnetId, address);
    }
}
