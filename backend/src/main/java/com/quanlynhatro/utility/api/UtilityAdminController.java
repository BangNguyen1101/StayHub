package com.quanlynhatro.utility.api;

import com.quanlynhatro.room.api.PageResponse;
import com.quanlynhatro.utility.service.FeeConfigService;
import com.quanlynhatro.utility.service.ServiceManagementService;
import com.quanlynhatro.utility.service.UtilityReadingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class UtilityAdminController {
    private final FeeConfigService feeConfigs;
    private final ServiceManagementService services;
    private final UtilityReadingService readings;

    public UtilityAdminController(
            FeeConfigService feeConfigs,
            ServiceManagementService services,
            UtilityReadingService readings) {
        this.feeConfigs = feeConfigs;
        this.services = services;
        this.readings = readings;
    }

    @GetMapping("/fee-configs")
    public PageResponse<FeeConfigResponse> searchFeeConfigs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) FeePricingMode pricingMode,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return PageResponse.from(feeConfigs.search(keyword, pricingMode, active, pageable));
    }

    @PostMapping("/fee-configs")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeConfigResponse createFeeConfig(
            @Valid @RequestBody FeeConfigCreateRequest request, Authentication authentication) {
        return feeConfigs.create(request, authentication);
    }

    @PatchMapping("/fee-configs/{id}")
    public FeeConfigResponse updateFeeConfig(
            @PathVariable @Positive Long id,
            @Valid @RequestBody FeeConfigUpdateRequest request,
            Authentication authentication) {
        return feeConfigs.update(id, request, authentication);
    }

    @GetMapping("/services")
    public PageResponse<ServiceResponse> searchServices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) FeePricingMode pricingMode,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return PageResponse.from(services.search(keyword, pricingMode, active, pageable));
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse createService(
            @Valid @RequestBody ServiceCreateRequest request, Authentication authentication) {
        return services.create(request, authentication);
    }

    @PatchMapping("/services/{id}")
    public ServiceResponse updateService(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ServiceUpdateRequest request,
            Authentication authentication) {
        return services.update(id, request, authentication);
    }

    @GetMapping("/utility-readings")
    public PageResponse<UtilityReadingResponse> searchReadings(
            @RequestParam(required = false) @Positive Long roomId,
            @RequestParam(required = false) UtilityType utilityType,
            @RequestParam(required = false) Boolean locked,
            Pageable pageable) {
        return PageResponse.from(readings.search(roomId, utilityType, locked, pageable));
    }

    @PostMapping("/utility-readings")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilityReadingResponse createReading(
            @Valid @RequestBody UtilityReadingCreateRequest request,
            Authentication authentication) {
        return readings.create(request, authentication);
    }

    @PatchMapping("/utility-readings/{id}")
    public UtilityReadingResponse updateReading(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UtilityReadingUpdateRequest request,
            Authentication authentication) {
        return readings.update(id, request, authentication);
    }

    @PatchMapping("/utility-readings/{id}/lock")
    public UtilityReadingResponse lockReading(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UtilityReadingLockRequest request,
            Authentication authentication) {
        return readings.lock(id, request, authentication);
    }
}
