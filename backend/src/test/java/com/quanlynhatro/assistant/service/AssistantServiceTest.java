package com.quanlynhatro.assistant.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.billing.api.DebtResponse;
import com.quanlynhatro.billing.api.InvoiceStatus;
import com.quanlynhatro.billing.service.BillingService;
import com.quanlynhatro.billing.service.PaymentService;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.maintenance.service.MaintenanceService;
import com.quanlynhatro.report.api.ReportDtos;
import com.quanlynhatro.report.service.ReportService;
import com.quanlynhatro.room.api.RoomOccupantResponse;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;
import com.quanlynhatro.room.service.RoomService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

class AssistantServiceTest {
    private AssistantIntentClassifier classifier;
    private AppUserRepository users;
    private BillingService billing;
    private ContractRepository contracts;
    private ReportService reports;
    private PaymentService payments;
    private MaintenanceService maintenance;
    private AssistantService service;

    @BeforeEach
    void setUp() {
        classifier = mock(AssistantIntentClassifier.class);
        users = mock(AppUserRepository.class);
        billing = mock(BillingService.class);
        contracts = mock(ContractRepository.class);
        reports = mock(ReportService.class);
        payments = mock(PaymentService.class);
        maintenance = mock(MaintenanceService.class);
        service =
                new AssistantService(
                        classifier, users, billing, contracts, reports, payments, maintenance);
    }

    @Test
    void adminDebtAnswerUsesRepositoryAmountsExactly() {
        AppUserEntity admin = user(1L, "ADMIN");
        authenticate("admin", admin);
        when(classifier.classify(anyString(), eq("ADMIN")))
                .thenReturn(AssistantIntent.ADMIN_MONTHLY_DEBT);
        String period = YearMonth.now().toString();
        DebtResponse debt =
                new DebtResponse(
                        10L,
                        "INV-001",
                        20L,
                        30L,
                        "A101",
                        40L,
                        "Nguyễn Văn A",
                        period,
                        LocalDate.now().plusDays(5),
                        new BigDecimal("1500000"),
                        new BigDecimal("250000"),
                        new BigDecimal("1250000"),
                        InvoiceStatus.PARTIALLY_PAID,
                        0);
        when(billing.debts(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(debt)));

        String answer =
                service.chat(auth("admin"), "Tháng này còn bao nhiêu tiền chưa thu?").answer();

        assertTrue(answer.contains("1.250.000 đ"));
        assertTrue(answer.contains("A101"));
        assertTrue(answer.contains("INV-001"));
        assertTrue(answer.contains("Nguyễn Văn A"));
    }

    @Test
    void tenantQueriesAreAlwaysScopedToAuthenticatedUser() {
        AppUserEntity tenant = user(42L, "TENANT");
        authenticate("tenant", tenant);
        when(classifier.classify(anyString(), eq("TENANT")))
                .thenReturn(AssistantIntent.TENANT_INVOICES);
        when(billing.debts(isNull(), eq(42L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        String answer = service.chat(auth("tenant"), "Tôi còn nợ bao nhiêu?").answer();

        assertTrue(answer.contains("không có hóa đơn nào còn nợ"));
        verify(billing).debts(isNull(), eq(42L), any(Pageable.class));
        verify(billing, never()).debts(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void tenantCannotExecuteAdminIntentEvenIfClassifierReturnsIt() {
        AppUserEntity tenant = user(42L, "TENANT");
        authenticate("tenant", tenant);
        when(classifier.classify(anyString(), eq("TENANT")))
                .thenReturn(AssistantIntent.ADMIN_MONTHLY_DEBT);

        assertThrows(
                AccessDeniedException.class,
                () -> service.chat(auth("tenant"), "Cho tôi xem nợ của tất cả phòng"));
        verifyNoInteractions(billing, contracts, reports, payments, maintenance);
    }

    @Test
    void adminOverviewUsesReportServiceData() {
        AppUserEntity admin = user(1L, "ADMIN");
        authenticate("admin", admin);
        when(classifier.classify(anyString(), eq("ADMIN")))
                .thenReturn(AssistantIntent.ADMIN_OVERVIEW);
        when(reports.dashboard(any(LocalDate.class)))
                .thenReturn(
                        new ReportDtos.Dashboard(
                                30,
                                21,
                                9,
                                new BigDecimal("70"),
                                20,
                                3,
                                1,
                                25,
                                2,
                                List.of(),
                                4,
                                1,
                                new BigDecimal("3200000")));

        String answer = service.chat(auth("admin"), "Tổng quan nhà trọ").answer();

        assertTrue(answer.contains("30 tổng"));
        assertTrue(answer.contains("9 còn trống"));
        assertTrue(answer.contains("3.200.000 đ"));
    }

    @Test
    void tenantMaintenanceIsScopedToAuthenticatedUser() {
        AppUserEntity tenant = user(42L, "TENANT");
        authenticate("tenant", tenant);
        when(classifier.classify(anyString(), eq("TENANT")))
                .thenReturn(AssistantIntent.TENANT_MAINTENANCE);
        when(maintenance.searchForTenant(any(), eq(42L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        String answer = service.chat(auth("tenant"), "Yêu cầu bảo trì của tôi?").answer();

        assertTrue(answer.contains("chưa gửi yêu cầu bảo trì"));
        verify(maintenance)
                .searchForTenant(
                        argThat(query -> Long.valueOf(42L).equals(query.reporterUserId())),
                        eq(42L),
                        any(Pageable.class));
    }

    @Test
    void geminiToolsRunInTheTenantScopeAndCannotReachAdminData() {
        GeminiAssistantAgent agent = mock(GeminiAssistantAgent.class);
        AssistantService ai = withAgent(agent);
        AppUserEntity tenant = user(42L, "TENANT");
        authenticate("tenant", tenant);
        when(agent.enabled()).thenReturn(true);
        when(billing.debts(isNull(), eq(42L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(agent.answer(eq("TENANT"), any(), any(), anyString(), any()))
                .thenAnswer(
                        invocation -> {
                            java.util.function.BiFunction<String, JsonNode, String> tool =
                                    invocation.getArgument(4);
                            String result =
                                    tool.apply(
                                            "tenant_invoices",
                                            JsonNodeFactory.instance.objectNode());
                            assertTrue(result.contains("không có hóa đơn nào còn nợ"));
                            assertThrows(
                                    IllegalArgumentException.class,
                                    () ->
                                            tool.apply(
                                                    "admin_monthly_debt",
                                                    JsonNodeFactory.instance.objectNode()));
                            return new GeminiAssistantAgent.Result(
                                    "Bạn không còn nợ.", List.of(AssistantIntent.TENANT_INVOICES));
                        });

        var response = ai.chat(auth("tenant"), "Tôi còn nợ gì không?", List.of());

        assertEquals("AI", response.mode());
        assertEquals("Bạn không còn nợ.", response.answer());
        assertEquals("/tenant/invoices", response.actions().get(0).path());
        assertFalse(response.suggestions().isEmpty());
        verify(billing, never()).debts(isNull(), isNull(), any(Pageable.class));
        verifyNoInteractions(classifier);
    }

    @Test
    void geminiFailureFallsBackToKeywordAnswer() {
        GeminiAssistantAgent agent = mock(GeminiAssistantAgent.class);
        AssistantService ai = withAgent(agent);
        AppUserEntity tenant = user(42L, "TENANT");
        authenticate("tenant", tenant);
        when(agent.enabled()).thenReturn(true);
        when(agent.answer(any(), any(), any(), anyString(), any()))
                .thenThrow(new IllegalStateException("timeout"));
        when(classifier.classify(anyString(), eq("TENANT")))
                .thenReturn(AssistantIntent.TENANT_INVOICES);
        when(billing.debts(isNull(), eq(42L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var response = ai.chat(auth("tenant"), "Tôi còn nợ bao nhiêu?", List.of());

        assertEquals("BASIC", response.mode());
        assertTrue(response.answer().contains("không có hóa đơn nào còn nợ"));
        assertEquals("Thanh toán ngay", response.actions().get(0).label());
    }

    @Test
    void geminiToolArgumentsAreValidated() {
        GeminiAssistantAgent agent = mock(GeminiAssistantAgent.class);
        AssistantService ai = withAgent(agent);
        AppUserEntity admin = user(1L, "ADMIN");
        authenticate("admin", admin);
        when(agent.enabled()).thenReturn(true);
        when(agent.answer(eq("ADMIN"), any(), any(), anyString(), any()))
                .thenAnswer(
                        invocation -> {
                            java.util.function.BiFunction<String, JsonNode, String> tool =
                                    invocation.getArgument(4);
                            var badPeriod = JsonNodeFactory.instance.objectNode().put("period", "13/2026");
                            assertThrows(
                                    IllegalArgumentException.class,
                                    () -> tool.apply("admin_utilities", badPeriod));
                            var badDays = JsonNodeFactory.instance.objectNode().put("days", 9999);
                            assertThrows(
                                    IllegalArgumentException.class,
                                    () -> tool.apply("admin_expiring_contracts", badDays));
                            return new GeminiAssistantAgent.Result("ok", List.of());
                        });

        var response = ai.chat(auth("admin"), "Điện nước tháng 13?", List.of());

        assertEquals("AI", response.mode());
        verifyNoInteractions(reports, contracts);
    }

    @Test
    void keywordModeLooksUpARoomMentionedByCode() {
        RoomRepository rooms = mock(RoomRepository.class);
        RoomService roomService = mock(RoomService.class);
        AssistantService basic =
                new AssistantService(
                        classifier, users, billing, contracts, reports, payments, maintenance,
                        null, rooms, roomService, null);
        AppUserEntity admin = user(1L, "ADMIN");
        authenticate("admin", admin);
        RoomEntity room = mock(RoomEntity.class);
        when(room.getId()).thenReturn(5L);
        when(room.getRoomCode()).thenReturn("A102");
        when(room.getStatus()).thenReturn("FULL");
        when(room.getMaxOccupants()).thenReturn(2);
        when(room.getBaseRent()).thenReturn(new BigDecimal("2400000"));
        when(rooms.findByRoomCodeIgnoreCase("A102")).thenReturn(Optional.of(room));
        when(roomService.currentOccupants(5L))
                .thenReturn(
                        List.of(
                                new RoomOccupantResponse(
                                        1L, 2L, "Nguyễn Lương Bằng", "0888072104", null, 3L,
                                        "HD03", true, LocalDate.of(2026, 11, 5), null)));
        when(billing.debts(eq("A102"), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var response = basic.chat(auth("admin"), "Phòng a102 ai đang ở vậy?");

        assertEquals("ADMIN_ROOM_DETAIL", response.intent());
        assertTrue(response.answer().contains("Nguyễn Lương Bằng (người thuê chính)"));
        assertTrue(response.answer().contains("Không có hoá đơn còn nợ"));
        verifyNoInteractions(classifier);
    }

    private AssistantService withAgent(GeminiAssistantAgent agent) {
        return new AssistantService(
                classifier, users, billing, contracts, reports, payments, maintenance, agent,
                null, null, null);
    }

    private void authenticate(String name, AppUserEntity user) {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase(name, name))
                .thenReturn(Optional.of(user));
    }

    private UsernamePasswordAuthenticationToken auth(String name) {
        return new UsernamePasswordAuthenticationToken(name, "n/a", List.of());
    }

    private AppUserEntity user(Long id, String roleCode) {
        AppUserEntity user = mock(AppUserEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(user.isActive()).thenReturn(true);
        when(role.getCode()).thenReturn(roleCode);
        return user;
    }
}
