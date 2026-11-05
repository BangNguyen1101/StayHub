package com.quanlynhatro.contract.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.repository.TenantProfileRepository;
import com.quanlynhatro.contract.api.ContractDocumentStatus;
import com.quanlynhatro.contract.api.ContractDocumentType;
import com.quanlynhatro.contract.api.ContractSignRequest;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractDocumentEntity;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.repository.ContractDocumentRepository;
import com.quanlynhatro.contract.repository.ContractHistoryRepository;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.contract.repository.SystemConfigRepository;
import com.quanlynhatro.notification.repository.NotificationRepository;
import com.quanlynhatro.notification.service.NotificationService;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import javax.imageio.ImageIO;

@ExtendWith(MockitoExtension.class)
class ContractDocumentServiceImplTest {
    @Mock private ContractRepository contracts;
    @Mock private ContractDocumentRepository documents;
    @Mock private ContractOccupantRepository occupants;
    @Mock private ContractHistoryRepository histories;
    @Mock private RoomRepository rooms;
    @Mock private AppUserRepository users;
    @Mock private TenantProfileRepository tenantProfiles;
    @Mock private SystemConfigRepository configs;
    @Mock private NotificationRepository notifications;
    @Mock private NotificationService notificationService;
    @TempDir Path storage;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ContractPdfRenderer renderer = new ContractPdfRenderer();
    private AppUserEntity admin;
    private AppUserEntity tenant;
    private RoomEntity room;

    @BeforeEach
    void setUp() {
        admin = user(1L, "Chủ Nhà Trọ");
        tenant = user(2L, "Nguyễn Lương Bằng");
        room = org.mockito.Mockito.mock(RoomEntity.class);
        lenient().when(room.getId()).thenReturn(5L);
        lenient().when(room.getRoomCode()).thenReturn("A102");
        lenient().when(room.getRoomName()).thenReturn("Phòng A102");
        lenient().when(room.getAreaM2()).thenReturn(new BigDecimal("18.00"));
        lenient().when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
    }

    @Test
    void previewPdfContainsVietnameseContractTerms() throws Exception {
        ContractEntity contract = contract(ContractStatus.PENDING, LocalDate.of(2027, 5, 1));
        ContractDocumentEntity document = issuedContractDocument(contract);
        when(documents.findById(7L)).thenReturn(Optional.of(document));

        var pdf = service().pdf(7L, new ContractDocumentService.Viewer(2L, false));

        String text = new PdfTextExtractor(new PdfReader(pdf.content())).getTextFromPage(1);
        assertTrue(text.contains("HỢP ĐỒNG THUÊ PHÒNG TRỌ"), text);
        assertTrue(text.contains("Nguyễn Lương Bằng"), text);
        assertTrue(text.contains("2.400.000 đ"), text);
        assertTrue(pdf.fileName().endsWith("-ban-xem-truoc.pdf"));
    }

    @Test
    void tenantSignsContractAndStoredPdfMatchesRecordedHash() throws Exception {
        ContractEntity contract = contract(ContractStatus.PENDING, LocalDate.of(2027, 5, 1));
        ContractDocumentEntity document = issuedContractDocument(contract);
        stubSigning(contract, document);

        var response =
                service()
                        .sign(
                                7L,
                                new ContractSignRequest(signature(true), true),
                                2L,
                                "10.0.0.8",
                                "Mobile Safari");

        assertEquals(ContractDocumentStatus.SIGNED, response.status());
        assertEquals("10.0.0.8", response.signerIp());
        byte[] stored = Files.readAllBytes(storage.resolve(document.getSignedPdfPath()));
        assertEquals(sha256(stored), response.signedPdfHash());
        verify(histories).save(any());
        verify(notificationService)
                .notifyAdmins(eq("CONTRACT_SIGNED"), anyString(), anyString(), eq("CONTRACT"), eq(10L));

        when(documents.findById(7L)).thenReturn(Optional.of(document));
        var viewer = new ContractDocumentService.Viewer(1L, true);
        assertTrue(service().verifyStored(7L, viewer).valid());

        // Any change to the stored file after signing must be detected.
        stored[stored.length / 2] ^= 0x01;
        Files.write(storage.resolve(document.getSignedPdfPath()), stored);
        assertFalse(service().verifyStored(7L, viewer).valid());
        assertFalse(service().verifyUpload(7L, "fake".getBytes(), viewer).valid());
    }

    @Test
    void anotherTenantCannotSign() {
        ContractEntity contract = contract(ContractStatus.PENDING, LocalDate.of(2027, 5, 1));
        ContractDocumentEntity document = issuedContractDocument(contract);
        stubSigning(contract, document);

        assertThrows(
                ContractAccessDeniedException.class,
                () ->
                        service()
                                .sign(
                                        7L,
                                        new ContractSignRequest(signature(true), true),
                                        99L,
                                        "ip",
                                        "ua"));
        assertEquals(ContractDocumentStatus.AWAITING_SIGNATURE, document.getStatus());
    }

    @Test
    void blankSignatureIsRejected() {
        ContractEntity contract = contract(ContractStatus.PENDING, LocalDate.of(2027, 5, 1));
        ContractDocumentEntity document = issuedContractDocument(contract);
        stubSigning(contract, document);

        assertThrows(
                ContractBusinessException.class,
                () ->
                        service()
                                .sign(
                                        7L,
                                        new ContractSignRequest(signature(false), true),
                                        2L,
                                        "ip",
                                        "ua"));
        assertEquals(ContractDocumentStatus.AWAITING_SIGNATURE, document.getStatus());
    }

    @Test
    void tamperedSnapshotCannotBeSigned() {
        ContractEntity contract = contract(ContractStatus.PENDING, LocalDate.of(2027, 5, 1));
        ContractDocumentEntity document = issuedContractDocument(contract);
        ReflectionTestUtils.setField(document, "contentSnapshot", "{\"monthlyRent\":1}");
        stubSigning(contract, document);

        assertThrows(
                ContractBusinessException.class,
                () ->
                        service()
                                .sign(
                                        7L,
                                        new ContractSignRequest(signature(true), true),
                                        2L,
                                        "ip",
                                        "ua"));
    }

    @Test
    void tenantRequestsRenewalForExpiringContract() {
        LocalDate end = LocalDate.now().plusDays(10);
        ContractEntity contract = contract(ContractStatus.EXPIRING, end);
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));

        var response = service().requestRenewal(10L, 6, 2L);

        ArgumentCaptor<ContractDocumentEntity> saved =
                ArgumentCaptor.forClass(ContractDocumentEntity.class);
        verify(documents).save(saved.capture());
        assertEquals(ContractDocumentType.RENEWAL, saved.getValue().getDocumentType());
        assertEquals(end.plusMonths(6), response.newEndDate());
        assertEquals(6, response.renewalMonths());
    }

    @Test
    void renewalIsOnlyForExpiringContractsOfTheTenant() {
        ContractEntity active = contract(ContractStatus.ACTIVE, LocalDate.now().plusMonths(6));
        when(contracts.findById(10L)).thenReturn(Optional.of(active));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(active));

        assertThrows(ContractBusinessException.class, () -> service().requestRenewal(10L, 6, 2L));
        assertThrows(
                ContractAccessDeniedException.class, () -> service().requestRenewal(10L, 6, 3L));
        assertThrows(ContractBusinessException.class, () -> service().requestRenewal(10L, 40, 2L));
        verify(documents, never()).save(any());
    }

    @Test
    void signingRenewalExtendsContractAndReactivatesIt() {
        LocalDate end = LocalDate.now().plusDays(10);
        ContractEntity contract = contract(ContractStatus.EXPIRING, end);
        ContractDocumentEntity document = renewalDocument(contract, end, 12);
        stubSigning(contract, document);

        service().sign(7L, new ContractSignRequest(signature(true), true), 2L, "ip", "ua");

        verify(contract).renewUntil(end.plusMonths(12));
        verify(contract).changeStatus(ContractStatus.ACTIVE);
        assertEquals(ContractDocumentStatus.SIGNED, document.getStatus());
    }

    @Test
    void renewalCannotBeSignedAfterEndDateChanged() {
        LocalDate end = LocalDate.now().plusDays(10);
        ContractEntity contract = contract(ContractStatus.EXPIRING, end);
        ContractDocumentEntity document = renewalDocument(contract, end.minusDays(1), 12);
        stubSigning(contract, document);

        assertThrows(
                ContractBusinessException.class,
                () ->
                        service()
                                .sign(
                                        7L,
                                        new ContractSignRequest(signature(true), true),
                                        2L,
                                        "ip",
                                        "ua"));
        verify(contract, never()).renewUntil(any());
    }

    @Test
    void outsiderTenantCannotDownloadDocument() {
        ContractEntity contract = contract(ContractStatus.PENDING, LocalDate.of(2027, 5, 1));
        ContractDocumentEntity document = issuedContractDocument(contract);
        when(documents.findById(7L)).thenReturn(Optional.of(document));
        when(occupants.existsByContract_IdAndUser_Id(10L, 50L)).thenReturn(false);

        assertThrows(
                ContractAccessDeniedException.class,
                () -> service().pdf(7L, new ContractDocumentService.Viewer(50L, false)));
    }

    @Test
    void signedContractBlocksHardDelete() {
        when(documents.existsByContract_IdAndStatus(10L, ContractDocumentStatus.SIGNED))
                .thenReturn(true);

        assertThrows(ContractBusinessException.class, () -> service().deleteForContract(10L));
        verify(documents, never()).deleteByContract_Id(anyLong());
    }

    private ContractDocumentServiceImpl service() {
        return new ContractDocumentServiceImpl(
                contracts,
                documents,
                occupants,
                histories,
                rooms,
                users,
                tenantProfiles,
                configs,
                notifications,
                notificationService,
                renderer,
                objectMapper,
                storage.toString());
    }

    private void stubSigning(ContractEntity contract, ContractDocumentEntity document) {
        lenient().when(documents.findById(7L)).thenReturn(Optional.of(document));
        lenient().when(documents.findByIdForUpdate(7L)).thenReturn(Optional.of(document));
        lenient().when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        lenient().when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
    }

    private ContractDocumentEntity issuedContractDocument(ContractEntity contract) {
        String snapshot = snapshot(content(contract, "CONTRACT", null, null, null));
        ContractDocumentEntity document =
                ContractDocumentEntity.issueContract(
                        contract, tenant, snapshot, sha256(snapshot.getBytes(StandardCharsets.UTF_8)), admin);
        ReflectionTestUtils.setField(document, "id", 7L);
        return document;
    }

    private ContractDocumentEntity renewalDocument(
            ContractEntity contract, LocalDate previousEnd, int months) {
        LocalDate newEnd = previousEnd.plusMonths(months);
        String snapshot = snapshot(content(contract, "RENEWAL", previousEnd, newEnd, months));
        ContractDocumentEntity document =
                ContractDocumentEntity.issueRenewal(
                        contract,
                        tenant,
                        previousEnd,
                        newEnd,
                        months,
                        snapshot,
                        sha256(snapshot.getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(document, "id", 7L);
        return document;
    }

    private ContractDocumentContent content(
            ContractEntity contract,
            String type,
            LocalDate previousEnd,
            LocalDate newEnd,
            Integer months) {
        return new ContractDocumentContent(
                type,
                "HD03",
                LocalDate.of(2026, 11, 5),
                new ContractDocumentContent.Party(
                        "Chủ Nhà Trọ", "0900000001", "admin@example.com", null, null, null, null,
                        null),
                new ContractDocumentContent.Party(
                        "Nguyễn Lương Bằng", "0888072104", "tenant@example.com", "CCCD",
                        "001204000000", LocalDate.of(2022, 1, 1), "Hà Nội", "Hà Nội"),
                new ContractDocumentContent.RoomInfo("A102", "Phòng A102", 1, new BigDecimal("18")),
                contract.getStartDate(),
                contract.getEndDate(),
                new BigDecimal("2400000.00"),
                new BigDecimal("2400000.00"),
                1,
                5,
                null,
                previousEnd,
                newEnd,
                months);
    }

    private ContractEntity contract(ContractStatus status, LocalDate endDate) {
        ContractEntity contract = org.mockito.Mockito.mock(ContractEntity.class);
        lenient().when(contract.getId()).thenReturn(10L);
        lenient().when(contract.getContractCode()).thenReturn("HD03");
        lenient().when(contract.getTenantUser()).thenReturn(tenant);
        lenient().when(contract.getCreatedByUser()).thenReturn(admin);
        lenient().when(contract.getRoom()).thenReturn(room);
        lenient().when(contract.getStatus()).thenReturn(status);
        lenient().when(contract.getStartDate()).thenReturn(endDate.minusMonths(12));
        lenient().when(contract.getEndDate()).thenReturn(endDate);
        lenient().when(contract.getMonthlyRent()).thenReturn(new BigDecimal("2400000.00"));
        lenient().when(contract.getDepositAmount()).thenReturn(new BigDecimal("2400000.00"));
        lenient().when(contract.getBillingCycleMonths()).thenReturn(1);
        lenient().when(contract.getDueDay()).thenReturn((short) 5);
        return contract;
    }

    private AppUserEntity user(Long id, String fullName) {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getFullName()).thenReturn(fullName);
        return user;
    }

    private String snapshot(ContractDocumentContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String signature(boolean withInk) {
        BufferedImage image = new BufferedImage(300, 120, BufferedImage.TYPE_INT_ARGB);
        if (withInk) {
            Graphics2D g = image.createGraphics();
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(4));
            g.drawLine(20, 90, 140, 30);
            g.drawLine(140, 30, 280, 100);
            g.dispose();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
