package com.quanlynhatro.utility.service;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.utility.api.FeeConfigCreateRequest;
import com.quanlynhatro.utility.api.FeeConfigResponse;
import com.quanlynhatro.utility.api.FeeConfigUpdateRequest;
import com.quanlynhatro.utility.api.FeePricingMode;
import com.quanlynhatro.utility.domain.FeeConfigEntity;
import com.quanlynhatro.utility.repository.FeeConfigRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class FeeConfigServiceImpl implements FeeConfigService {
    private final FeeConfigRepository configs;
    private final AppUserRepository users;

    public FeeConfigServiceImpl(FeeConfigRepository configs, AppUserRepository users) {
        this.configs = configs;
        this.users = users;
    }

    @Override
    public Page<FeeConfigResponse> search(
            String keyword, FeePricingMode pricingMode, Boolean active, Pageable pageable) {
        return configs.search(normalize(keyword), mode(pricingMode), active, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FeeConfigResponse create(FeeConfigCreateRequest request, Authentication authentication) {
        validateDatesAndPrice(request.effectiveFrom(), request.effectiveTo(), request.unitPrice());
        String code = normalizeRequired(request.feeCode());
        if (configs.existsOverlappingPeriod(
                code, request.effectiveFrom(), request.effectiveTo(), null)) {
            throw new FeeConfigPeriodOverlapException(code);
        }
        AppUserEntity actor = actor(authentication);
        FeeConfigEntity entity =
                FeeConfigEntity.create(
                        code,
                        request.feeName().trim(),
                        request.unitName().trim(),
                        request.unitPrice(),
                        request.pricingMode().name(),
                        request.effectiveFrom(),
                        request.effectiveTo(),
                        request.active(),
                        actor);
        return toResponse(configs.save(entity));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FeeConfigResponse update(
            Long id, FeeConfigUpdateRequest request, Authentication authentication) {
        validateDatesAndPrice(request.effectiveFrom(), request.effectiveTo(), request.unitPrice());
        FeeConfigEntity entity =
                configs.findByIdForUpdate(id).orElseThrow(() -> new FeeConfigNotFoundException(id));
        if (configs.existsOverlappingPeriod(
                entity.getFeeCode(), request.effectiveFrom(), request.effectiveTo(), id)) {
            throw new FeeConfigPeriodOverlapException(entity.getFeeCode());
        }
        actor(authentication);
        entity.updateDetails(
                request.feeName().trim(),
                request.unitName().trim(),
                request.unitPrice(),
                request.pricingMode().name(),
                request.effectiveFrom(),
                request.effectiveTo(),
                request.active());
        return toResponse(entity);
    }

    @Override
    public FeeConfigResponse findEffective(String feeCode, LocalDate atDate) {
        if (atDate == null) throw new UtilityBusinessException("Effective date is required");
        return configs.findEffective(normalizeRequired(feeCode), atDate)
                .map(this::toResponse)
                .orElseThrow(() -> new FeeConfigNotFoundException(feeCode));
    }

    private AppUserEntity actor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UtilityBusinessException("Authenticated user is required");
        }
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        authentication.getName(), authentication.getName())
                .orElseThrow(
                        () -> new UtilityBusinessException("Authenticated user was not found"));
    }

    private void validateDatesAndPrice(LocalDate from, LocalDate to, BigDecimal price) {
        if (from == null || (to != null && to.isBefore(from))) {
            throw new InvalidFeeConfigPeriodException();
        }
        if (price == null || price.signum() < 0) throw new InvalidFeePriceException();
    }

    private FeeConfigResponse toResponse(FeeConfigEntity entity) {
        return new FeeConfigResponse(
                entity.getId(),
                entity.getFeeCode(),
                entity.getFeeName(),
                entity.getUnitName(),
                entity.getUnitPrice(),
                FeePricingMode.valueOf(entity.getPricingMode()),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                entity.isActive(),
                entity.getCreatedAt());
    }

    private String mode(FeePricingMode value) {
        return value == null ? null : value.name();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty())
            throw new UtilityBusinessException("FeeCode is required");
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
