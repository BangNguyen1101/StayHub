package com.quanlynhatro.maintenance.api;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.maintenance.service.MaintenanceAccessDeniedException;
import com.quanlynhatro.maintenance.service.MaintenanceService;
import com.quanlynhatro.room.api.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MaintenanceController {
    private final MaintenanceService maintenance;
    private final AppUserRepository users;

    public MaintenanceController(MaintenanceService maintenance, AppUserRepository users) {
        this.maintenance = maintenance;
        this.users = users;
    }

    @PostMapping("/maintenance-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceResponse create(
            Authentication authentication, @Valid @RequestBody MaintenanceCreateRequest request) {
        return maintenance.createForTenant(request, current(authentication, "TENANT").getId());
    }

    @GetMapping("/maintenance-requests")
    public PageResponse<MaintenanceResponse> tenantList(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) MaintenancePriority priority,
            @RequestParam(required = false) MaintenanceIssueType issueType,
            Pageable pageable) {
        AppUserEntity tenant = current(authentication, "TENANT");
        MaintenanceQuery query =
                new MaintenanceQuery(
                        keyword, status, priority, issueType, null, null, null, null, null);
        return PageResponse.from(maintenance.searchForTenant(query, tenant.getId(), pageable));
    }

    @GetMapping("/maintenance-requests/{id}")
    public MaintenanceResponse tenantDetail(
            Authentication authentication, @PathVariable @Positive Long id) {
        return maintenance.getForTenant(id, current(authentication, "TENANT").getId());
    }

    @PostMapping(
            value = "/maintenance-requests/{id}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<MaintenanceAttachmentResponse> uploadAttachments(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @RequestPart("files") List<MultipartFile> files) {
        return maintenance.uploadImages(id, files, current(authentication, "TENANT").getId());
    }

    @GetMapping("/maintenance-attachments/{id}")
    public ResponseEntity<InputStreamResource> tenantAttachment(
            Authentication authentication, @PathVariable @Positive Long id) throws IOException {
        return fileResponse(
                maintenance.downloadForTenant(id, current(authentication, "TENANT").getId()));
    }

    @GetMapping("/admin/maintenance-requests")
    public PageResponse<MaintenanceResponse> adminList(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) MaintenancePriority priority,
            @RequestParam(required = false) MaintenanceIssueType issueType,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long reporterUserId,
            @RequestParam(required = false) Long assignedToUserId,
            Pageable pageable) {
        MaintenanceQuery query =
                new MaintenanceQuery(
                        keyword,
                        status,
                        priority,
                        issueType,
                        roomId,
                        reporterUserId,
                        assignedToUserId,
                        null,
                        null);
        return PageResponse.from(
                maintenance.searchForAdmin(
                        query, current(authentication, "ADMIN").getId(), pageable));
    }

    @GetMapping("/admin/maintenance-requests/{id}")
    public MaintenanceResponse adminDetail(
            Authentication authentication, @PathVariable @Positive Long id) {
        return maintenance.getForAdmin(id, current(authentication, "ADMIN").getId());
    }

    @GetMapping("/admin/maintenance-assignees")
    public List<MaintenanceAssigneeResponse> assignees(Authentication authentication) {
        current(authentication, "ADMIN");
        return users.findAllByRole_CodeAndActiveTrue("ADMIN").stream()
                .map(user -> new MaintenanceAssigneeResponse(user.getId(), user.getFullName()))
                .toList();
    }

    @GetMapping("/admin/maintenance-attachments/{id}")
    public ResponseEntity<InputStreamResource> adminAttachment(
            Authentication authentication, @PathVariable @Positive Long id) throws IOException {
        return fileResponse(
                maintenance.downloadForAdmin(id, current(authentication, "ADMIN").getId()));
    }

    @PatchMapping("/admin/maintenance-requests/{id}")
    public MaintenanceResponse update(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody MaintenanceAdminUpdateRequest request) {
        return maintenance.updateForAdmin(id, request, current(authentication, "ADMIN").getId());
    }

    @GetMapping("/admin/maintenance-requests/{id}/history")
    public List<MaintenanceHistoryResponse> history(
            Authentication authentication, @PathVariable @Positive Long id) {
        return maintenance.historyForAdmin(id, current(authentication, "ADMIN").getId());
    }

    private AppUserEntity current(Authentication authentication, String role) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new MaintenanceAccessDeniedException();
        }
        AppUserEntity user =
                users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                authentication.getName(), authentication.getName())
                        .orElseThrow(MaintenanceAccessDeniedException::new);
        if (user.getRole() == null || !role.equals(user.getRole().getCode()) || !user.isActive()) {
            throw new MaintenanceAccessDeniedException();
        }
        return user;
    }

    private ResponseEntity<InputStreamResource> fileResponse(
            MaintenanceService.MaintenanceFile file) throws IOException {
        MediaType mediaType = MediaType.parseMediaType(file.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(java.nio.file.Files.size(file.path()))
                .body(new InputStreamResource(java.nio.file.Files.newInputStream(file.path())));
    }
}
