package com.quanlynhatro.maintenance.service;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.maintenance.api.*;
import com.quanlynhatro.maintenance.domain.*;
import com.quanlynhatro.maintenance.repository.*;
import com.quanlynhatro.notification.service.NotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;

@Service
public class MaintenanceServiceImpl implements MaintenanceService {
    private final MaintenanceRequestRepository requests;
    private final MaintenanceRequestHistoryRepository histories;
    private final AppUserRepository users;
    private final ContractRepository contracts;
    private final ContractOccupantRepository occupants;
    private final NotificationService notifications;
    private FileAttachmentRepository attachments;
    private Path uploadRoot = Path.of("./uploads/maintenance").toAbsolutePath().normalize();
    private long maxImageSizeBytes = 5_242_880L;

    public MaintenanceServiceImpl(
            MaintenanceRequestRepository requests,
            MaintenanceRequestHistoryRepository histories,
            AppUserRepository users,
            ContractRepository contracts,
            ContractOccupantRepository occupants,
            NotificationService notifications) {
        this.requests = requests;
        this.histories = histories;
        this.users = users;
        this.contracts = contracts;
        this.occupants = occupants;
        this.notifications = notifications;
    }

    @Autowired
    void configureAttachments(
            FileAttachmentRepository attachments,
            @Value("${app.maintenance.upload-dir:./uploads/maintenance}") String uploadDir,
            @Value("${app.maintenance.max-image-size-bytes:5242880}") long maxImageSizeBytes) {
        this.attachments = attachments;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    @Override
    @Transactional
    public MaintenanceResponse createForTenant(MaintenanceCreateRequest request, Long tenantId) {
        AppUserEntity tenant = requireActiveRole(tenantId, "TENANT");
        LocalDate today = LocalDate.now();
        List<ContractEntity> candidates = new ArrayList<>(contracts.findByTenantUser_Id(tenantId));
        // A tenant added as a co-occupant is not stored in Contracts.TenantUserId.
        // Include every contract relationship so a logged-in tenant can report
        // maintenance even while the admin is still completing the contract workflow.
        candidates.addAll(occupants.findContractsByUserId(tenantId));
        ContractEntity contract = selectMaintenanceContract(candidates, today);
        String code = nextRequestCode();
        MaintenanceRequestEntity entity =
                requests.save(
                        MaintenanceRequestEntity.create(
                                code, tenant, contract.getRoom(), contract, request));
        histories.save(
                MaintenanceRequestHistoryEntity.create(
                        entity,
                        null,
                        MaintenanceStatus.OPEN,
                        "Người thuê đã gửi yêu cầu bảo trì",
                        tenant));
        notifications.notifyAdmins(
                "MAINTENANCE_REQUEST",
                "Yêu cầu bảo trì mới tại phòng " + contract.getRoom().getRoomCode(),
                "Người thuê đã báo "
                        + issueTypeLabel(request.issueType())
                        + ": "
                        + request.title()
                        + ". Mã yêu cầu "
                        + code
                        + ". Vui lòng mở mục Bảo trì để kiểm tra.",
                "MAINTENANCE_REQUEST",
                entity.getId());
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> searchForTenant(
            MaintenanceQuery query, Long tenantId, Pageable pageable) {
        requireActiveRole(tenantId, "TENANT");
        return search(query, tenantId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceResponse getForTenant(Long id, Long tenantId) {
        requireActiveRole(tenantId, "TENANT");
        return requests.findById(id)
                .filter(r -> r.getReporterUser().getId().equals(tenantId))
                .map(this::toResponse)
                .orElseThrow(MaintenanceNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> searchForAdmin(
            MaintenanceQuery query, Long adminId, Pageable pageable) {
        requireActiveRole(adminId, "ADMIN");
        return search(query, null, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceResponse getForAdmin(Long id, Long adminId) {
        requireActiveRole(adminId, "ADMIN");
        return requests.findById(id)
                .map(this::toResponse)
                .orElseThrow(MaintenanceNotFoundException::new);
    }

    @Override
    @Transactional
    public MaintenanceResponse updateForAdmin(
            Long id, MaintenanceAdminUpdateRequest request, Long adminId) {
        AppUserEntity admin = requireActiveRole(adminId, "ADMIN");
        MaintenanceRequestEntity entity =
                requests.findByIdForUpdate(id).orElseThrow(MaintenanceNotFoundException::new);
        MaintenanceStatus old = entity.getStatus();
        validateTransition(old, request.status());
        if (request.status() == MaintenanceStatus.REJECTED && blank(request.rejectionReason())) {
            throw new MaintenanceConflictException("Rejection reason is required");
        }
        if (request.status() == MaintenanceStatus.RESOLVED && blank(request.resolutionNote())) {
            throw new MaintenanceConflictException("Resolution note is required");
        }
        AppUserEntity assignee =
                request.assignedToUserId() == null
                        ? null
                        : requireActiveRole(request.assignedToUserId(), "ADMIN");
        entity.applyAdminUpdate(request, assignee, LocalDateTime.now(ZoneOffset.UTC));
        histories.save(
                MaintenanceRequestHistoryEntity.create(
                        entity, old, request.status(), request.note(), admin));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceHistoryResponse> historyForAdmin(Long id, Long adminId) {
        requireActiveRole(adminId, "ADMIN");
        if (!requests.existsById(id)) throw new MaintenanceNotFoundException();
        return histories.findByRequest_IdOrderByChangedAtAsc(id).stream()
                .map(
                        h ->
                                new MaintenanceHistoryResponse(
                                        h.getId(),
                                        h.getRequest().getId(),
                                        h.getOldStatus(),
                                        h.getNewStatus(),
                                        h.getNote(),
                                        h.getChangedByUser().getId(),
                                        h.getChangedAt()))
                .toList();
    }

    @Override
    @Transactional
    public List<MaintenanceAttachmentResponse> uploadImages(
            Long id, List<MultipartFile> files, Long tenantId) {
        requireAttachmentRepository();
        AppUserEntity tenant = requireActiveRole(tenantId, "TENANT");
        MaintenanceRequestEntity request =
                requests.findById(id)
                        .filter(item -> item.getReporterUser().getId().equals(tenantId))
                        .orElseThrow(MaintenanceNotFoundException::new);
        if (request.getStatus() == MaintenanceStatus.CLOSED
                || request.getStatus() == MaintenanceStatus.REJECTED) {
            throw new MaintenanceConflictException("Cannot add images to a closed request");
        }
        if (files == null || files.isEmpty() || files.size() > 5)
            throw new MaintenanceBusinessException("Chỉ được gửi từ 1 đến 5 ảnh.");
        List<MaintenanceAttachmentResponse> result = new ArrayList<>();
        for (MultipartFile file : files) {
            validateImage(file);
            try {
                Files.createDirectories(uploadRoot);
                String extension = extensionOf(file.getOriginalFilename());
                String storedName = UUID.randomUUID() + extension;
                Path target = uploadRoot.resolve(storedName).normalize();
                if (!target.startsWith(uploadRoot))
                    throw new MaintenanceBusinessException("Đường dẫn ảnh không hợp lệ.");
                byte[] bytes = file.getBytes();
                Files.write(target, bytes);
                FileAttachmentEntity saved =
                        attachments.save(
                                FileAttachmentEntity.create(
                                        request,
                                        tenant,
                                        safeOriginalName(file.getOriginalFilename()),
                                        storedName,
                                        target.toString(),
                                        file.getContentType(),
                                        bytes.length,
                                        sha256(bytes)));
                result.add(toAttachmentResponse(saved));
            } catch (IOException ex) {
                throw new MaintenanceBusinessException(
                        "Không thể lưu ảnh sự cố. Vui lòng thử lại.");
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceFile downloadForTenant(Long attachmentId, Long tenantId) {
        requireAttachmentRepository();
        requireActiveRole(tenantId, "TENANT");
        FileAttachmentEntity attachment =
                attachments.findById(attachmentId).orElseThrow(MaintenanceNotFoundException::new);
        if (!attachment.getRequest().getReporterUser().getId().equals(tenantId))
            throw new MaintenanceNotFoundException();
        return fileOf(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceFile downloadForAdmin(Long attachmentId, Long adminId) {
        requireAttachmentRepository();
        requireActiveRole(adminId, "ADMIN");
        return fileOf(
                attachments.findById(attachmentId).orElseThrow(MaintenanceNotFoundException::new));
    }

    private Page<MaintenanceRequestEntity> search(
            MaintenanceQuery q, Long reporterId, Pageable pageable) {
        q =
                q == null
                        ? new MaintenanceQuery(null, null, null, null, null, null, null, null, null)
                        : q;
        LocalDateTime from = q.createdFrom() == null ? null : q.createdFrom().atStartOfDay();
        LocalDateTime to = q.createdTo() == null ? null : q.createdTo().plusDays(1).atStartOfDay();
        return requests.search(
                normalize(q.keyword()),
                q.status(),
                q.priority(),
                q.issueType(),
                q.roomId(),
                reporterId == null ? q.reporterUserId() : reporterId,
                q.assignedToUserId(),
                from,
                to,
                pageable);
    }

    private void validateTransition(MaintenanceStatus from, MaintenanceStatus to) {
        if (from == to) {
            if (from == MaintenanceStatus.CLOSED || from == MaintenanceStatus.REJECTED)
                throw new MaintenanceConflictException("Closed requests cannot be changed");
            return;
        }
        boolean valid =
                switch (from) {
                    case OPEN ->
                            to == MaintenanceStatus.IN_PROGRESS || to == MaintenanceStatus.REJECTED;
                    case IN_PROGRESS ->
                            to == MaintenanceStatus.WAITING || to == MaintenanceStatus.RESOLVED;
                    case WAITING ->
                            to == MaintenanceStatus.IN_PROGRESS || to == MaintenanceStatus.RESOLVED;
                    case RESOLVED -> to == MaintenanceStatus.CLOSED;
                    case CLOSED, REJECTED -> false;
                };
        if (!valid)
            throw new MaintenanceConflictException(
                    "Invalid maintenance status transition: " + from + " -> " + to);
    }

    private AppUserEntity requireActiveRole(Long id, String role) {
        AppUserEntity user =
                users.findByIdAndRole_Code(id, role)
                        .orElseThrow(MaintenanceAccessDeniedException::new);
        if (!user.isActive()) throw new MaintenanceAccessDeniedException();
        return user;
    }

    private String nextRequestCode() {
        for (int i = 0; i < 5; i++) {
            String code =
                    "MR-"
                            + LocalDate.now()
                                    .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                            + "-"
                            + UUID.randomUUID()
                                    .toString()
                                    .replace("-", "")
                                    .substring(0, 8)
                                    .toUpperCase();
            if (!requests.existsByRequestCodeIgnoreCase(code)) return code;
        }
        throw new MaintenanceConflictException("Could not generate a unique request code");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ContractEntity selectMaintenanceContract(
            List<ContractEntity> candidates, LocalDate today) {
        Map<Long, ContractEntity> unique = new LinkedHashMap<>();
        List<ContractEntity> withoutId = new ArrayList<>();
        for (ContractEntity candidate : candidates) {
            if (candidate == null) continue;
            if (candidate.getId() == null) withoutId.add(candidate);
            else unique.putIfAbsent(candidate.getId(), candidate);
        }
        List<ContractEntity> available = new ArrayList<>(unique.values());
        available.addAll(withoutId);
        return available.stream()
                .sorted(
                        Comparator.comparing((ContractEntity c) -> isWithinContractDates(c, today))
                                .reversed()
                                .thenComparing(c -> isOpenContract(c.getStatus()) ? 0 : 1)
                                .thenComparing(
                                        ContractEntity::getEndDate,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(
                                        ContractEntity::getId,
                                        Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElseThrow(
                        () ->
                                new MaintenanceConflictException(
                                        "Tài khoản chưa được gán vào phòng nào. Hãy liên hệ quản lý"
                                            + " để gán phòng trước khi báo bảo trì."));
    }

    private boolean isWithinContractDates(ContractEntity contract, LocalDate today) {
        return contract.getStartDate() != null
                && contract.getEndDate() != null
                && !contract.getStartDate().isAfter(today)
                && !contract.getEndDate().isBefore(today);
    }

    private boolean isOpenContract(ContractStatus status) {
        return status == ContractStatus.ACTIVE
                || status == ContractStatus.EXPIRING
                || status == ContractStatus.PENDING
                || status == ContractStatus.DRAFT;
    }

    private String issueTypeLabel(MaintenanceIssueType type) {
        return switch (type) {
            case ELECTRICITY -> "mất điện hoặc hỏng ổ cắm";
            case WATER -> "mất nước hoặc rò rỉ nước";
            case EQUIPMENT -> "hỏng thiết bị khác";
            case OTHER -> "sự cố khác";
        };
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new MaintenanceBusinessException("Ảnh đính kèm không được để trống.");
        if (file.getSize() > maxImageSizeBytes)
            throw new MaintenanceBusinessException("Mỗi ảnh tối đa 5 MB.");
        if (file.getContentType() == null
                || !Set.of("image/jpeg", "image/png", "image/webp")
                        .contains(file.getContentType().toLowerCase(Locale.ROOT))) {
            throw new MaintenanceBusinessException("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.");
        }
    }

    private void requireAttachmentRepository() {
        if (attachments == null)
            throw new IllegalStateException("Attachment storage is not configured");
    }

    private MaintenanceFile fileOf(FileAttachmentEntity attachment) {
        Path path = Path.of(attachment.getStoragePath()).toAbsolutePath().normalize();
        if (!path.startsWith(uploadRoot) || !Files.isRegularFile(path))
            throw new MaintenanceNotFoundException();
        return new MaintenanceFile(
                attachment.getOriginalFileName(), attachment.getContentType(), path);
    }

    private String safeOriginalName(String name) {
        return name == null
                ? "image"
                : Path.of(name).getFileName().toString().replaceAll("[^a-zA-Z0-9._ -]", "_");
    }

    private String extensionOf(String name) {
        String safe = safeOriginalName(name);
        int dot = safe.lastIndexOf('.');
        return dot < 0 ? "" : safe.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private MaintenanceAttachmentResponse toAttachmentResponse(FileAttachmentEntity item) {
        return new MaintenanceAttachmentResponse(
                item.getId(),
                item.getOriginalFileName(),
                item.getContentType(),
                item.getFileSizeBytes(),
                item.getCreatedAt());
    }

    private MaintenanceResponse toResponse(MaintenanceRequestEntity m) {
        return new MaintenanceResponse(
                m.getId(),
                m.getRequestCode(),
                m.getReporterUser().getId(),
                m.getRoom().getId(),
                m.getRoom().getRoomCode(),
                m.getContract() == null ? null : m.getContract().getId(),
                m.getIssueType(),
                m.getTitle(),
                m.getDescription(),
                m.getPriority(),
                m.getStatus(),
                m.getAssignedToUser() == null ? null : m.getAssignedToUser().getId(),
                m.getEstimatedCost(),
                m.getActualCost(),
                m.getResolutionNote(),
                m.getRejectionReason(),
                m.getCreatedAt(),
                m.getUpdatedAt(),
                m.getResolvedAt(),
                m.getReporterUser().getFullName(),
                m.getAssignedToUser() == null ? null : m.getAssignedToUser().getFullName(),
                attachments == null
                        ? List.of()
                        : attachments.findByRequest_IdOrderByCreatedAtAsc(m.getId()).stream()
                                .map(this::toAttachmentResponse)
                                .toList());
    }
}
