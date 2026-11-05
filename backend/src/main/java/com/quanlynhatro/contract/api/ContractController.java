package com.quanlynhatro.contract.api;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.contract.service.ContractAccessDeniedException;
import com.quanlynhatro.contract.service.ContractManagementService;
import com.quanlynhatro.contract.service.ContractOccupantService;
import com.quanlynhatro.room.api.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ContractController {
    private static final String ADMIN = "ADMIN";

    private final ContractManagementService contracts;
    private final ContractOccupantService occupants;
    private final AppUserRepository users;

    public ContractController(
            ContractManagementService contracts,
            ContractOccupantService occupants,
            AppUserRepository users) {
        this.contracts = contracts;
        this.occupants = occupants;
        this.users = users;
    }

    @GetMapping("/admin/contracts")
    public PageResponse<ContractSummaryResponse> search(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) Long tenantUserId,
            @RequestParam(required = false) Long roomId,
            Pageable pageable) {
        requireCurrentAdmin(authentication);
        return PageResponse.from(contracts.search(keyword, status, tenantUserId, roomId, pageable));
    }

    @GetMapping("/admin/contracts/{id}")
    public ContractResponse getById(
            Authentication authentication, @PathVariable @Positive Long id) {
        requireCurrentAdmin(authentication);
        return contracts.getById(id);
    }

    @PostMapping("/admin/contracts")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse create(
            Authentication authentication, @Valid @RequestBody ContractCreateRequest request) {
        return contracts.create(request, requireCurrentAdmin(authentication).getId());
    }

    @PatchMapping("/admin/contracts/{id}")
    public ContractResponse update(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody ContractUpdateRequest request) {
        return contracts.updateDraft(id, request, requireCurrentAdmin(authentication).getId());
    }

    @PatchMapping("/admin/contracts/{id}/submit")
    public ContractResponse submit(Authentication authentication, @PathVariable @Positive Long id) {
        return contracts.submitPending(id, requireCurrentAdmin(authentication).getId());
    }

    @PatchMapping("/admin/contracts/{id}/activate")
    public ContractResponse activate(
            Authentication authentication, @PathVariable @Positive Long id) {
        return contracts.activate(id, requireCurrentAdmin(authentication).getId());
    }

    @PatchMapping("/admin/contracts/{id}/renew")
    public ContractResponse renew(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody ContractRenewRequest request) {
        return contracts.renew(id, request, requireCurrentAdmin(authentication).getId());
    }

    @PatchMapping("/admin/contracts/{id}/terminate")
    public ContractResponse terminate(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody ContractTerminateRequest request) {
        return contracts.terminate(id, request, requireCurrentAdmin(authentication).getId());
    }

    @PatchMapping("/admin/contracts/{id}/cancel")
    public ContractResponse cancel(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody ContractCancelRequest request) {
        return contracts.cancel(id, request, requireCurrentAdmin(authentication).getId());
    }

    @DeleteMapping("/admin/contracts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOldContract(Authentication authentication, @PathVariable @Positive Long id) {
        contracts.deleteOldContract(id, requireCurrentAdmin(authentication).getId());
    }

    @GetMapping("/admin/contracts/{id}/history")
    public List<ContractHistoryResponse> history(
            Authentication authentication, @PathVariable @Positive Long id) {
        requireCurrentAdmin(authentication);
        return contracts.history(id);
    }

    @GetMapping("/admin/contracts/{id}/occupants")
    public List<ContractOccupantResponse> listOccupants(
            Authentication authentication, @PathVariable @Positive Long id) {
        requireCurrentAdmin(authentication);
        return occupants.list(id);
    }

    @PostMapping("/admin/contracts/{id}/occupants")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractOccupantResponse addOccupant(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody ContractOccupantCreateRequest request) {
        return occupants.add(id, request, requireCurrentAdmin(authentication).getId());
    }

    @PatchMapping("/admin/contracts/occupants/{occupantId}")
    public ContractOccupantResponse updateOccupant(
            Authentication authentication,
            @PathVariable @Positive Long occupantId,
            @Valid @RequestBody ContractOccupantUpdateRequest request) {
        return occupants.update(occupantId, request, requireCurrentAdmin(authentication).getId());
    }

    @PatchMapping("/admin/contracts/occupants/{occupantId}/move-out")
    public ContractOccupantResponse moveOut(
            Authentication authentication,
            @PathVariable @Positive Long occupantId,
            @Valid @RequestBody ContractOccupantMoveOutRequest request) {
        return occupants.moveOut(
                occupantId, request.moveOutDate(), requireCurrentAdmin(authentication).getId());
    }

    @DeleteMapping("/admin/contracts/occupants/{occupantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeOccupant(
            Authentication authentication, @PathVariable @Positive Long occupantId) {
        occupants.remove(occupantId, requireCurrentAdmin(authentication).getId());
    }

    @GetMapping("/tenant/contracts")
    public PageResponse<ContractSummaryResponse> mine(
            Authentication authentication, Pageable pageable) {
        Long userId = currentUser(authentication).getId();
        return PageResponse.from(contracts.search(null, null, userId, null, pageable));
    }

    @GetMapping("/tenant/contracts/{id}")
    public ContractResponse mineById(
            Authentication authentication, @PathVariable @Positive Long id) {
        return contracts.getForTenant(authentication.getName(), id);
    }

    private AppUserEntity requireCurrentAdmin(Authentication authentication) {
        AppUserEntity user = currentUser(authentication);
        if (user.getRole() == null || !ADMIN.equals(user.getRole().getCode())) {
            throw new ContractAccessDeniedException();
        }
        return user;
    }

    private AppUserEntity currentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ContractAccessDeniedException();
        }
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        authentication.getName(), authentication.getName())
                .orElseThrow(ContractAccessDeniedException::new);
    }
}
