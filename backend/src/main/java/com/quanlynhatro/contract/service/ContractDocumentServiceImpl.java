package com.quanlynhatro.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.TenantProfileEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.repository.TenantProfileRepository;
import com.quanlynhatro.contract.api.ContractDocumentResponse;
import com.quanlynhatro.contract.api.ContractDocumentStatus;
import com.quanlynhatro.contract.api.ContractDocumentType;
import com.quanlynhatro.contract.api.ContractDocumentVerifyResponse;
import com.quanlynhatro.contract.api.ContractSignRequest;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractDocumentEntity;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.domain.ContractHistoryEntity;
import com.quanlynhatro.contract.repository.ContractDocumentRepository;
import com.quanlynhatro.contract.repository.ContractHistoryRepository;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.contract.repository.SystemConfigRepository;
import com.quanlynhatro.notification.domain.NotificationEntity;
import com.quanlynhatro.notification.repository.NotificationRepository;
import com.quanlynhatro.notification.service.NotificationService;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

@Service
@Transactional(readOnly = true)
public class ContractDocumentServiceImpl implements ContractDocumentService {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PNG_DATA_URL_PREFIX = "data:image/png;base64,";
    private static final int MAX_SIGNATURE_BYTES = 500 * 1024;
    private static final int MIN_SIGNATURE_INK_PIXELS = 40;
    private static final int DEFAULT_WARNING_DAYS = 30;

    private final ContractRepository contracts;
    private final ContractDocumentRepository documents;
    private final ContractOccupantRepository occupants;
    private final ContractHistoryRepository histories;
    private final RoomRepository rooms;
    private final AppUserRepository users;
    private final TenantProfileRepository tenantProfiles;
    private final SystemConfigRepository configs;
    private final NotificationRepository notifications;
    private final NotificationService notificationService;
    private final ContractPdfRenderer renderer;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    public ContractDocumentServiceImpl(
            ContractRepository contracts,
            ContractDocumentRepository documents,
            ContractOccupantRepository occupants,
            ContractHistoryRepository histories,
            RoomRepository rooms,
            AppUserRepository users,
            TenantProfileRepository tenantProfiles,
            SystemConfigRepository configs,
            NotificationRepository notifications,
            NotificationService notificationService,
            ContractPdfRenderer renderer,
            ObjectMapper objectMapper,
            @Value("${app.contracts.document-dir:./storage/contract-documents}")
                    String storageDir) {
        this.contracts = contracts;
        this.documents = documents;
        this.occupants = occupants;
        this.histories = histories;
        this.rooms = rooms;
        this.users = users;
        this.tenantProfiles = tenantProfiles;
        this.configs = configs;
        this.notifications = notifications;
        this.notificationService = notificationService;
        this.renderer = renderer;
        this.objectMapper = objectMapper;
        this.storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
    }

    @Override
    public List<ContractDocumentResponse> list(Long contractId, Viewer viewer) {
        ContractEntity contract =
                contracts
                        .findById(contractId)
                        .orElseThrow(() -> new ContractNotFoundException(contractId));
        requireViewable(contract, viewer);
        return documents.findByContract_IdOrderByCreatedAtDesc(contractId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ContractDocumentResponse issueForAdmin(Long contractId, Long adminUserId) {
        AppUserEntity admin =
                users.findByIdAndRole_Code(adminUserId, ADMIN_ROLE)
                        .orElseThrow(ContractAccessDeniedException::new);
        ContractEntity contract = lockContract(contractId);
        if (contract.getStatus() != ContractStatus.PENDING) {
            throw new ContractBusinessException("Chỉ gửi ký được hợp đồng đang chờ duyệt (PENDING)");
        }
        issueContractDocument(contract, admin);
        return documents
                .findByContract_IdAndStatus(contractId, ContractDocumentStatus.AWAITING_SIGNATURE)
                .or(() -> latestSignedContract(contractId))
                .map(this::toResponse)
                .orElseThrow(() -> new ContractBusinessException("Không thể tạo tài liệu ký"));
    }

    @Override
    @Transactional
    public void issueContractDocument(ContractEntity contract, AppUserEntity admin) {
        Long contractId = contract.getId();
        if (documents.existsByContract_IdAndDocumentTypeAndStatus(
                        contractId, ContractDocumentType.CONTRACT, ContractDocumentStatus.SIGNED)
                || documents
                        .findByContract_IdAndStatus(
                                contractId, ContractDocumentStatus.AWAITING_SIGNATURE)
                        .isPresent()) {
            return;
        }
        ContractDocumentContent content =
                buildContent(contract, ContractDocumentType.CONTRACT, null, null, null);
        String snapshot = serialize(content);
        ContractDocumentEntity document =
                ContractDocumentEntity.issueContract(
                        contract, contract.getTenantUser(), snapshot, sha256(snapshot), admin);
        documents.save(document);
        writeHistory(contract, "DOCUMENT_ISSUED", null, documentSnapshot(document), admin);
        notifications.save(
                NotificationEntity.create(
                        contract.getTenantUser(),
                        "CONTRACT_SIGNATURE",
                        "Hợp đồng đang chờ bạn ký",
                        "Hợp đồng " + contract.getContractCode() + " cho phòng "
                                + contract.getRoom().getRoomCode()
                                + " đang chờ bạn xem và ký điện tử.",
                        "CONTRACT",
                        contractId));
    }

    @Override
    @Transactional
    public ContractDocumentResponse requestRenewal(
            Long contractId, int months, Long tenantUserId) {
        if (months < 1 || months > 36) {
            throw new ContractBusinessException("Số tháng gia hạn phải từ 1 đến 36");
        }
        ContractEntity contract = lockContract(contractId);
        if (!contract.getTenantUser().getId().equals(tenantUserId)) {
            throw new ContractAccessDeniedException();
        }
        if (contract.getStatus() != ContractStatus.EXPIRING) {
            throw new ContractBusinessException("Chỉ gia hạn được khi hợp đồng sắp hết hạn");
        }
        documents
                .findByContract_IdAndStatus(contractId, ContractDocumentStatus.AWAITING_SIGNATURE)
                .ifPresent(
                        pending -> {
                            if (pending.getDocumentType() != ContractDocumentType.RENEWAL) {
                                throw new ContractBusinessException(
                                        "Hợp đồng đang có tài liệu khác chờ ký");
                            }
                            // Choosing another duration replaces the unsigned appendix. Flush
                            // first: only one document may be awaiting signature per contract.
                            pending.markVoid();
                            documents.saveAndFlush(pending);
                        });

        LocalDate previousEnd = contract.getEndDate();
        LocalDate newEnd = previousEnd.plusMonths(months);
        ContractDocumentContent content =
                buildContent(
                        contract, ContractDocumentType.RENEWAL, previousEnd, newEnd, months);
        String snapshot = serialize(content);
        ContractDocumentEntity document =
                ContractDocumentEntity.issueRenewal(
                        contract,
                        contract.getTenantUser(),
                        previousEnd,
                        newEnd,
                        months,
                        snapshot,
                        sha256(snapshot));
        documents.save(document);
        writeHistory(
                contract,
                "RENEWAL_REQUESTED",
                null,
                documentSnapshot(document),
                contract.getTenantUser());
        return toResponse(document);
    }

    @Override
    @Transactional
    public ContractDocumentResponse sign(
            Long documentId,
            ContractSignRequest request,
            Long tenantUserId,
            String signerIp,
            String signerUserAgent) {
        ContractDocumentEntity initial = findDocument(documentId);
        ContractEntity contract = lockContract(initial.getContract().getId());
        ContractDocumentEntity document =
                documents
                        .findByIdForUpdate(documentId)
                        .orElseThrow(() -> documentNotFound(documentId));
        AppUserEntity signer = document.getSignerUser();
        if (!signer.getId().equals(tenantUserId)) {
            throw new ContractAccessDeniedException();
        }
        if (document.getStatus() != ContractDocumentStatus.AWAITING_SIGNATURE) {
            throw new ContractBusinessException("Tài liệu này không còn chờ ký");
        }
        ensureSignable(contract, document);
        if (!sha256(document.getContentSnapshot()).equals(document.getContentHash())) {
            throw new ContractBusinessException("Nội dung tài liệu không toàn vẹn, không thể ký");
        }

        byte[] signaturePng = normalizeSignature(request.signatureImage());
        LocalDateTime signedAt = LocalDateTime.now(ZoneOffset.UTC);
        String ip = truncate(signerIp, 64);
        byte[] pdf =
                renderer.render(
                        document.getId(),
                        deserialize(document.getContentSnapshot()),
                        document.getContentHash(),
                        new ContractPdfRenderer.SignatureStamp(
                                signaturePng, signer.getFullName(), signedAt, ip));
        String pdfHash = sha256(pdf);
        String storedPath = store(contract.getId(), document.getId(), pdf);
        document.markSigned(signedAt, ip, truncate(signerUserAgent, 500), storedPath, pdfHash);

        if (document.getDocumentType() == ContractDocumentType.RENEWAL) {
            String before = contractDates(contract);
            contract.renewUntil(document.getNewEndDate());
            if (contract.getStatus() == ContractStatus.EXPIRING
                    && document.getNewEndDate().isAfter(LocalDate.now().plusDays(warningDays()))) {
                contract.changeStatus(ContractStatus.ACTIVE);
            }
            writeHistory(contract, "RENEWED", before, contractDates(contract), signer);
        }
        writeHistory(contract, "DOCUMENT_SIGNED", null, documentSnapshot(document), signer);
        notificationService.notifyAdmins(
                "CONTRACT_SIGNED",
                document.getDocumentType() == ContractDocumentType.RENEWAL
                        ? "Người thuê đã ký phụ lục gia hạn"
                        : "Người thuê đã ký hợp đồng",
                signer.getFullName() + " đã ký điện tử "
                        + (document.getDocumentType() == ContractDocumentType.RENEWAL
                                ? "phụ lục gia hạn đến " + document.getNewEndDate()
                                : "hợp đồng")
                        + " (" + contract.getContractCode() + ").",
                "CONTRACT",
                contract.getId());
        return toResponse(document);
    }

    @Override
    public DocumentPdf pdf(Long documentId, Viewer viewer) {
        ContractDocumentEntity document = findDocument(documentId);
        requireViewable(document.getContract(), viewer);
        String fileName =
                document.getContract().getContractCode().replaceAll("[^A-Za-z0-9_-]", "_")
                        + (document.getDocumentType() == ContractDocumentType.RENEWAL
                                ? "-phu-luc-"
                                : "-hop-dong-")
                        + document.getId()
                        + (document.getStatus() == ContractDocumentStatus.SIGNED
                                ? ".pdf"
                                : "-ban-xem-truoc.pdf");
        if (document.getStatus() == ContractDocumentStatus.SIGNED) {
            return new DocumentPdf(fileName, readStored(document));
        }
        byte[] preview =
                renderer.render(
                        document.getId(),
                        deserialize(document.getContentSnapshot()),
                        document.getContentHash(),
                        null);
        return new DocumentPdf(fileName, preview);
    }

    @Override
    public ContractDocumentVerifyResponse verifyStored(Long documentId, Viewer viewer) {
        ContractDocumentEntity document = requireSigned(documentId, viewer);
        String actual;
        try {
            actual = sha256(readStored(document));
        } catch (ContractBusinessException ex) {
            actual = null;
        }
        return verification(document, actual);
    }

    @Override
    public ContractDocumentVerifyResponse verifyUpload(
            Long documentId, byte[] file, Viewer viewer) {
        ContractDocumentEntity document = requireSigned(documentId, viewer);
        if (file == null || file.length == 0) {
            throw new ContractBusinessException("Vui lòng chọn file PDF cần kiểm tra");
        }
        return verification(document, sha256(file));
    }

    @Override
    public boolean isContractSigned(Long contractId) {
        return documents.existsByContract_IdAndDocumentTypeAndStatus(
                contractId, ContractDocumentType.CONTRACT, ContractDocumentStatus.SIGNED);
    }

    @Override
    @Transactional
    public void voidAwaiting(Long contractId) {
        documents
                .findByContract_IdAndStatus(contractId, ContractDocumentStatus.AWAITING_SIGNATURE)
                .ifPresent(ContractDocumentEntity::markVoid);
    }

    @Override
    @Transactional
    public void deleteForContract(Long contractId) {
        if (documents.existsByContract_IdAndStatus(contractId, ContractDocumentStatus.SIGNED)) {
            throw new ContractBusinessException(
                    "Hợp đồng đã được ký điện tử, không thể xoá để giữ chứng từ");
        }
        documents.deleteByContract_Id(contractId);
    }

    private void ensureSignable(ContractEntity contract, ContractDocumentEntity document) {
        if (document.getDocumentType() == ContractDocumentType.CONTRACT) {
            if (contract.getStatus() != ContractStatus.PENDING) {
                throw new ContractBusinessException("Hợp đồng không còn ở trạng thái chờ ký");
            }
            return;
        }
        if (contract.getStatus() != ContractStatus.ACTIVE
                && contract.getStatus() != ContractStatus.EXPIRING) {
            throw new ContractBusinessException("Hợp đồng không còn hiệu lực để gia hạn");
        }
        if (!contract.getEndDate().equals(document.getPreviousEndDate())) {
            throw new ContractBusinessException(
                    "Thời hạn hợp đồng đã thay đổi, vui lòng tạo lại phụ lục gia hạn");
        }
    }

    private ContractDocumentContent buildContent(
            ContractEntity contract,
            ContractDocumentType type,
            LocalDate previousEndDate,
            LocalDate newEndDate,
            Integer renewalMonths) {
        AppUserEntity tenant = contract.getTenantUser();
        TenantProfileEntity profile = tenantProfiles.findByUserId(tenant.getId()).orElse(null);
        AppUserEntity landlord = contract.getCreatedByUser();
        RoomEntity room = contract.getRoom();
        return new ContractDocumentContent(
                type.name(),
                contract.getContractCode(),
                LocalDate.now(),
                new ContractDocumentContent.Party(
                        landlord.getFullName(),
                        landlord.getPhoneNumber(),
                        landlord.getEmail(),
                        null,
                        null,
                        null,
                        null,
                        null),
                new ContractDocumentContent.Party(
                        tenant.getFullName(),
                        tenant.getPhoneNumber(),
                        tenant.getEmail(),
                        profile == null ? null : profile.getIdentityDocumentType(),
                        profile == null ? null : profile.getIdentityDocumentNumber(),
                        profile == null ? null : profile.getIdentityIssuedDate(),
                        profile == null ? null : profile.getIdentityIssuedPlace(),
                        profile == null ? null : profile.getContactAddress()),
                new ContractDocumentContent.RoomInfo(
                        room.getRoomCode(),
                        room.getRoomName(),
                        room.getFloorNumber(),
                        room.getAreaM2()),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getMonthlyRent(),
                contract.getDepositAmount(),
                contract.getBillingCycleMonths(),
                contract.getDueDay() == null ? null : contract.getDueDay().intValue(),
                contract.getTerms(),
                previousEndDate,
                newEndDate,
                renewalMonths);
    }

    /** Validates the drawn signature and re-encodes it, so only clean PNG pixels are embedded. */
    private byte[] normalizeSignature(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith(PNG_DATA_URL_PREFIX)) {
            throw new ContractBusinessException("Chữ ký phải là ảnh PNG");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(dataUrl.substring(PNG_DATA_URL_PREFIX.length()));
        } catch (IllegalArgumentException ex) {
            throw new ContractBusinessException("Dữ liệu chữ ký không hợp lệ");
        }
        if (raw.length == 0 || raw.length > MAX_SIGNATURE_BYTES) {
            throw new ContractBusinessException("Ảnh chữ ký quá lớn hoặc rỗng");
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(raw));
            if (image == null
                    || image.getWidth() < 50
                    || image.getHeight() < 30
                    || image.getWidth() > 3000
                    || image.getHeight() > 3000) {
                throw new ContractBusinessException("Ảnh chữ ký không hợp lệ");
            }
            if (inkPixels(image) < MIN_SIGNATURE_INK_PIXELS) {
                throw new ContractBusinessException("Vui lòng ký tên trước khi xác nhận");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ContractBusinessException("Ảnh chữ ký không hợp lệ");
        }
    }

    /** Counts visible, dark pixels (signature strokes on a transparent or white background). */
    private static int inkPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                int luminance =
                        (((argb >> 16) & 0xff) * 299
                                        + ((argb >> 8) & 0xff) * 587
                                        + (argb & 0xff) * 114)
                                / 1000;
                if (alpha > 64 && luminance < 160) {
                    count++;
                }
            }
        }
        return count;
    }

    private String store(Long contractId, Long documentId, byte[] pdf) {
        String relative =
                "contract-" + contractId + "/document-" + documentId + "-" + UUID.randomUUID()
                        + ".pdf";
        Path target = storageRoot.resolve(relative).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new ContractBusinessException("Đường dẫn lưu tài liệu không hợp lệ");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, pdf, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            throw new ContractBusinessException("Không thể lưu file hợp đồng đã ký");
        }
        deleteOnRollback(target);
        return relative;
    }

    /** A signed PDF written to disk must not outlive a rolled-back signing transaction. */
    private static void deleteOnRollback(Path file) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try {
                                Files.deleteIfExists(file);
                            } catch (IOException ignored) {
                                // Orphan file only; the database never referenced it.
                            }
                        }
                    }
                });
    }

    private byte[] readStored(ContractDocumentEntity document) {
        Path path = storageRoot.resolve(document.getSignedPdfPath()).normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw new ContractBusinessException("Không tìm thấy file hợp đồng đã ký");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new ContractBusinessException("Không thể đọc file hợp đồng đã ký");
        }
    }

    private ContractDocumentEntity requireSigned(Long documentId, Viewer viewer) {
        ContractDocumentEntity document = findDocument(documentId);
        requireViewable(document.getContract(), viewer);
        if (document.getStatus() != ContractDocumentStatus.SIGNED) {
            throw new ContractBusinessException("Tài liệu chưa được ký");
        }
        return document;
    }

    private ContractDocumentVerifyResponse verification(
            ContractDocumentEntity document, String actualHash) {
        return new ContractDocumentVerifyResponse(
                document.getId(),
                document.getSignedPdfHash().equalsIgnoreCase(String.valueOf(actualHash)),
                document.getSignedPdfHash(),
                actualHash,
                LocalDateTime.now(ZoneOffset.UTC));
    }

    private void requireViewable(ContractEntity contract, Viewer viewer) {
        if (viewer.admin()) {
            return;
        }
        boolean primary = contract.getTenantUser().getId().equals(viewer.userId());
        if (!primary && !occupants.existsByContract_IdAndUser_Id(contract.getId(), viewer.userId())) {
            throw new ContractAccessDeniedException();
        }
    }

    /** Locks room then contract, the same order as the other contract operations. */
    private ContractEntity lockContract(Long contractId) {
        ContractEntity initial =
                contracts
                        .findById(contractId)
                        .orElseThrow(() -> new ContractNotFoundException(contractId));
        rooms.findByIdForUpdate(initial.getRoom().getId())
                .orElseThrow(() -> new ContractBusinessException("Room not found"));
        return contracts
                .findByIdForUpdate(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));
    }

    private ContractDocumentEntity findDocument(Long documentId) {
        return documents.findById(documentId).orElseThrow(() -> documentNotFound(documentId));
    }

    private static ContractBusinessException documentNotFound(Long documentId) {
        return new ContractBusinessException("Không tìm thấy tài liệu hợp đồng: " + documentId);
    }

    private java.util.Optional<ContractDocumentEntity> latestSignedContract(Long contractId) {
        return documents.findByContract_IdOrderByCreatedAtDesc(contractId).stream()
                .filter(d -> d.getDocumentType() == ContractDocumentType.CONTRACT)
                .filter(d -> d.getStatus() == ContractDocumentStatus.SIGNED)
                .findFirst();
    }

    private int warningDays() {
        return configs.findById("CONTRACT_EXPIRY_WARNING_DAYS")
                .map(
                        config -> {
                            try {
                                int days = Integer.parseInt(config.getValue());
                                return days >= 0 ? days : DEFAULT_WARNING_DAYS;
                            } catch (NumberFormatException ex) {
                                return DEFAULT_WARNING_DAYS;
                            }
                        })
                .orElse(DEFAULT_WARNING_DAYS);
    }

    private void writeHistory(
            ContractEntity contract,
            String action,
            String before,
            String after,
            AppUserEntity actor) {
        histories.save(ContractHistoryEntity.create(contract, action, before, after, actor));
    }

    private String documentSnapshot(ContractDocumentEntity document) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentId", document.getId());
        data.put("documentType", document.getDocumentType());
        data.put("status", document.getStatus());
        data.put("contentHash", document.getContentHash());
        data.put("renewalMonths", document.getRenewalMonths());
        data.put("newEndDate", document.getNewEndDate());
        data.put("signedPdfHash", document.getSignedPdfHash());
        return serialize(data);
    }

    private String contractDates(ContractEntity contract) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractId", contract.getId());
        data.put("endDate", contract.getEndDate());
        data.put("status", contract.getStatus());
        return serialize(data);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ContractBusinessException("Unable to serialize contract document");
        }
    }

    private ContractDocumentContent deserialize(String snapshot) {
        try {
            return objectMapper.readValue(snapshot, ContractDocumentContent.class);
        } catch (JsonProcessingException ex) {
            throw new ContractBusinessException("Nội dung tài liệu không đọc được");
        }
    }

    private ContractDocumentResponse toResponse(ContractDocumentEntity document) {
        return new ContractDocumentResponse(
                document.getId(),
                document.getContract().getId(),
                document.getContract().getContractCode(),
                document.getDocumentType(),
                document.getStatus(),
                document.getPreviousEndDate(),
                document.getNewEndDate(),
                document.getRenewalMonths(),
                document.getContentHash(),
                document.getSignerUser().getId(),
                document.getSignerUser().getFullName(),
                document.getSignedAt(),
                document.getSignerIp(),
                document.getSignedPdfHash(),
                document.getCreatedAt(),
                document.getVoidedAt());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
