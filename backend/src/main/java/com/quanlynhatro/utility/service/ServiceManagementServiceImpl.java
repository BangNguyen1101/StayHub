package com.quanlynhatro.utility.service;

import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.utility.api.FeePricingMode;
import com.quanlynhatro.utility.api.ServiceCreateRequest;
import com.quanlynhatro.utility.api.ServiceResponse;
import com.quanlynhatro.utility.api.ServiceUpdateRequest;
import com.quanlynhatro.utility.domain.ServiceEntity;
import com.quanlynhatro.utility.repository.ServiceRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ServiceManagementServiceImpl implements ServiceManagementService {
    private final ServiceRepository services;
    private final AppUserRepository users;

    public ServiceManagementServiceImpl(ServiceRepository services, AppUserRepository users) {
        this.services = services;
        this.users = users;
    }

    @Override
    public Page<ServiceResponse> search(
            String keyword, FeePricingMode pricingMode, Boolean active, Pageable pageable) {
        return services.search(
                        normalize(keyword),
                        pricingMode == null ? null : pricingMode.name(),
                        active,
                        pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ServiceResponse create(ServiceCreateRequest request, Authentication authentication) {
        String code = normalizeRequired(request.serviceCode());
        if (services.existsByServiceCodeIgnoreCase(code))
            throw new DuplicateServiceCodeException(code);
        actor(authentication);
        validatePrice(request.defaultUnitPrice());
        return toResponse(
                services.save(
                        ServiceEntity.create(
                                code,
                                request.serviceName().trim(),
                                request.pricingMode().name(),
                                request.defaultUnitPrice(),
                                request.active())));
    }

    @Override
    @Transactional
    public ServiceResponse update(
            Long id, ServiceUpdateRequest request, Authentication authentication) {
        ServiceEntity entity =
                services.findById(id).orElseThrow(() -> new ServiceNotFoundException(id));
        actor(authentication);
        validatePrice(request.defaultUnitPrice());
        entity.updateDetails(
                request.serviceName().trim(),
                request.pricingMode().name(),
                request.defaultUnitPrice(),
                request.active());
        return toResponse(entity);
    }

    private void actor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new UtilityBusinessException("Authenticated user is required");
        users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        authentication.getName(), authentication.getName())
                .orElseThrow(
                        () -> new UtilityBusinessException("Authenticated user was not found"));
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) throw new InvalidFeePriceException();
    }

    private ServiceResponse toResponse(ServiceEntity entity) {
        return new ServiceResponse(
                entity.getId(),
                entity.getServiceCode(),
                entity.getServiceName(),
                FeePricingMode.valueOf(entity.getPricingMode()),
                entity.getDefaultUnitPrice(),
                entity.isActive(),
                entity.getCreatedAt());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty())
            throw new UtilityBusinessException("ServiceCode is required");
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
