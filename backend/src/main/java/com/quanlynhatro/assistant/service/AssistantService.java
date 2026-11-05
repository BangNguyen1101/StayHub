package com.quanlynhatro.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.quanlynhatro.assistant.api.AssistantChatRequest.Turn;
import com.quanlynhatro.assistant.api.AssistantChatResponse;
import com.quanlynhatro.assistant.api.AssistantChatResponse.Action;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.billing.api.DebtResponse;
import com.quanlynhatro.billing.api.InvoiceItemResponse;
import com.quanlynhatro.billing.api.InvoiceItemType;
import com.quanlynhatro.billing.api.InvoiceResponse;
import com.quanlynhatro.billing.service.BillingService;
import com.quanlynhatro.billing.service.PaymentService;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.maintenance.api.MaintenanceQuery;
import com.quanlynhatro.maintenance.api.MaintenanceResponse;
import com.quanlynhatro.maintenance.service.MaintenanceService;
import com.quanlynhatro.report.api.ReportDtos;
import com.quanlynhatro.report.service.ReportService;
import com.quanlynhatro.room.api.RoomOccupantResponse;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;
import com.quanlynhatro.room.service.RoomService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AssistantService {
    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private static final Pattern ROOM_CODE =
            Pattern.compile("(?iu)ph[oò]ng\\s+([a-z]{1,3}\\s?-?\\d{1,4}[a-z]?|\\d{2,4})\\b");
    private static final List<String> ADMIN_DEFAULT_SUGGESTIONS =
            List.of(
                    "Tổng quan tình hình nhà trọ",
                    "Tháng này còn bao nhiêu tiền chưa thu?",
                    "Phòng nào sắp hết hợp đồng?");
    private static final List<String> TENANT_DEFAULT_SUGGESTIONS =
            List.of(
                    "Tôi còn hóa đơn nào chưa trả?",
                    "Hợp đồng của tôi còn hạn đến bao giờ?",
                    "Tiền điện nước gần đây của tôi?");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY =
            NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private static final Set<ContractStatus> ACTIVE =
            Set.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING);
    private final AssistantIntentClassifier classifier;
    private final AppUserRepository users;
    private final BillingService billing;
    private final ContractRepository contracts;
    private final ReportService reports;
    private final PaymentService payments;
    private final MaintenanceService maintenance;
    private final GeminiAssistantAgent agent;
    private final RoomRepository rooms;
    private final RoomService roomService;
    private final TransactionTemplate readOnlyTx;

    /** Keyword-only assistant without Gemini or room lookups (used by unit tests). */
    public AssistantService(
            AssistantIntentClassifier classifier,
            AppUserRepository users,
            BillingService billing,
            ContractRepository contracts,
            ReportService reports,
            PaymentService payments,
            MaintenanceService maintenance) {
        this(
                classifier, users, billing, contracts, reports, payments, maintenance, null, null,
                null, null);
    }

    @Autowired
    public AssistantService(
            AssistantIntentClassifier classifier,
            AppUserRepository users,
            BillingService billing,
            ContractRepository contracts,
            ReportService reports,
            PaymentService payments,
            MaintenanceService maintenance,
            GeminiAssistantAgent agent,
            RoomRepository rooms,
            RoomService roomService,
            PlatformTransactionManager transactionManager) {
        this.classifier = classifier;
        this.users = users;
        this.billing = billing;
        this.contracts = contracts;
        this.reports = reports;
        this.payments = payments;
        this.maintenance = maintenance;
        this.agent = agent;
        this.rooms = rooms;
        this.roomService = roomService;
        if (transactionManager == null) {
            this.readOnlyTx = null;
        } else {
            this.readOnlyTx = new TransactionTemplate(transactionManager);
            this.readOnlyTx.setReadOnly(true);
        }
    }

    public AssistantChatResponse chat(Authentication authentication, String message) {
        return chat(authentication, message, List.of());
    }

    /**
     * Answers with Gemini when it is configured, falling back to the keyword classifier when it is
     * not or when the Gemini call fails. No transaction is held while waiting for Gemini; each data
     * lookup runs in its own read-only transaction.
     */
    public AssistantChatResponse chat(
            Authentication authentication, String message, List<Turn> history) {
        AppUserEntity user = inTransaction(() -> current(authentication));
        String role = user.getRole().getCode();
        if (agent != null && agent.enabled()) {
            try {
                GeminiAssistantAgent.Result result =
                        agent.answer(
                                role,
                                user.getFullName(),
                                history,
                                message,
                                (name, args) -> runTool(user, role, name, args));
                return response(result.answer(), result.usedIntents(), role, "AI");
            } catch (AccessDeniedException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                log.warn(
                        "Gemini assistant unavailable, using keyword fallback: {}",
                        ex.getClass().getSimpleName());
            }
        }
        String roomCode = "ADMIN".equals(role) ? mentionedRoomCode(message) : null;
        AssistantIntent intent =
                roomCode != null
                        ? AssistantIntent.ADMIN_ROOM_DETAIL
                        : classifier.classify(message, role);
        Map<String, String> args = roomCode == null ? Map.of() : Map.of("roomCode", roomCode);
        String answer = inTransaction(() -> execute(intent, user, role, args::get));
        return response(
                answer,
                intent == AssistantIntent.UNSUPPORTED ? List.of() : List.of(intent),
                role,
                "BASIC");
    }

    /** Runs one lookup requested by the model; only the tools of the user's role exist. */
    private String runTool(AppUserEntity user, String role, String name, JsonNode args) {
        AssistantToolCatalog.Tool tool =
                AssistantToolCatalog.find(role, name)
                        .orElseThrow(() -> new IllegalArgumentException("Công cụ không tồn tại"));
        JsonNode safeArgs = args == null ? MissingNode.getInstance() : args;
        return inTransaction(
                () ->
                        execute(
                                tool.intent(),
                                user,
                                role,
                                key -> {
                                    JsonNode value = safeArgs.path(key);
                                    return value.isMissingNode() || value.isNull()
                                            ? null
                                            : value.asText();
                                }));
    }

    private String execute(
            AssistantIntent intent,
            AppUserEntity user,
            String role,
            java.util.function.Function<String, String> arg) {
        return switch (intent) {
            case ADMIN_MONTHLY_DEBT ->
                    requireAdmin(role, () -> adminMonthlyDebt(period(arg.apply("period"))));
            case ADMIN_EXPIRING_CONTRACTS ->
                    requireAdmin(role, () -> adminExpiringContracts(days(arg.apply("days"))));
            case ADMIN_OVERVIEW -> requireAdmin(role, this::adminOverview);
            case ADMIN_ROOMS -> requireAdmin(role, this::adminRooms);
            case ADMIN_ROOM_DETAIL ->
                    requireAdmin(role, () -> adminRoomDetail(roomCode(arg.apply("roomCode"))));
            case ADMIN_REVENUE ->
                    requireAdmin(
                            role,
                            () -> {
                                LocalDate today = LocalDate.now(ZONE);
                                LocalDate from = date(arg.apply("fromDate"), today.withDayOfMonth(1));
                                LocalDate to = date(arg.apply("toDate"), today);
                                if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 3660)
                                    throw new IllegalArgumentException(
                                            "Khoảng ngày không hợp lệ (tối đa 10 năm)");
                                return adminRevenue(from, to);
                            });
            case ADMIN_MAINTENANCE -> requireAdmin(role, this::adminMaintenance);
            case ADMIN_UTILITIES ->
                    requireAdmin(role, () -> adminUtilities(period(arg.apply("period"))));
            case TENANT_INVOICES -> requireTenant(role, () -> tenantInvoices(user.getId()));
            case TENANT_UTILITIES -> requireTenant(role, () -> tenantUtilities(user.getId()));
            case TENANT_CONTRACT -> requireTenant(role, () -> tenantContract(user.getId()));
            case TENANT_PAYMENT_DUE -> requireTenant(role, () -> tenantPaymentDue(user.getId()));
            case TENANT_PAYMENTS -> requireTenant(role, () -> tenantPayments(user.getId()));
            case TENANT_MAINTENANCE -> requireTenant(role, () -> tenantMaintenance(user.getId()));
            default -> unsupported(role);
        };
    }

    private AssistantChatResponse response(
            String answer, List<AssistantIntent> used, String role, String mode) {
        Map<String, Action> actions = new LinkedHashMap<>();
        List<String> suggestions = new ArrayList<>();
        for (AssistantIntent intent : used) {
            AssistantToolCatalog.of(intent)
                    .ifPresent(tool -> tool.actions().forEach(a -> actions.putIfAbsent(a.path(), a)));
        }
        if (!used.isEmpty()) {
            AssistantToolCatalog.of(used.get(used.size() - 1))
                    .ifPresent(tool -> suggestions.addAll(tool.suggestions()));
        }
        if (suggestions.isEmpty()) {
            suggestions.addAll(
                    "ADMIN".equals(role) ? ADMIN_DEFAULT_SUGGESTIONS : TENANT_DEFAULT_SUGGESTIONS);
        }
        return new AssistantChatResponse(
                answer,
                (used.isEmpty() ? AssistantIntent.UNSUPPORTED : used.get(0)).name(),
                LocalDateTime.now(ZONE),
                actions.values().stream().limit(3).toList(),
                suggestions.stream().distinct().limit(3).toList(),
                mode);
    }

    private <T> T inTransaction(Supplier<T> work) {
        return readOnlyTx == null ? work.get() : readOnlyTx.execute(status -> work.get());
    }

    private static String mentionedRoomCode(String message) {
        Matcher matcher = ROOM_CODE.matcher(message);
        return matcher.find() ? matcher.group(1).replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT) : null;
    }

    private static YearMonth period(String value) {
        if (value == null || value.isBlank()) return YearMonth.now(ZONE);
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Kỳ tháng phải có dạng yyyy-MM");
        }
    }

    private static LocalDate date(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Ngày phải có dạng yyyy-MM-dd");
        }
    }

    private static int days(String value) {
        if (value == null || value.isBlank()) return 30;
        try {
            int days = (int) Double.parseDouble(value.trim());
            if (days >= 1 && days <= 365) return days;
        } catch (NumberFormatException ignored) {
            // reported below
        }
        throw new IllegalArgumentException("Số ngày phải từ 1 đến 365");
    }

    private static String roomCode(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 50)
            throw new IllegalArgumentException("Cần mã phòng hợp lệ");
        return value.trim();
    }

    private String adminRoomDetail(String code) {
        if (rooms == null || roomService == null) return "Chưa hỗ trợ tra cứu chi tiết phòng.";
        RoomEntity room = rooms.findByRoomCodeIgnoreCase(code).orElse(null);
        if (room == null) return "Không tìm thấy phòng có mã " + code + ".";
        List<RoomOccupantResponse> occupants = roomService.currentOccupants(room.getId());
        StringBuilder out =
                new StringBuilder("Phòng ")
                        .append(room.getRoomCode())
                        .append(room.getRoomName() == null ? "" : " (" + room.getRoomName() + ")")
                        .append(" — ")
                        .append(roomStatus(room.getStatus()))
                        .append(", ")
                        .append(occupants.size())
                        .append('/')
                        .append(room.getMaxOccupants())
                        .append(" người, giá thuê ")
                        .append(money(room.getBaseRent()))
                        .append("/tháng\n");
        if (occupants.isEmpty()) {
            out.append("• Hiện chưa có người ở.\n");
        } else {
            occupants.forEach(
                    o ->
                            out.append("• ")
                                    .append(o.fullName())
                                    .append(o.primaryTenant() ? " (người thuê chính)" : " (ở cùng)")
                                    .append(" — HĐ ")
                                    .append(o.contractCode())
                                    .append(" — vào ở ")
                                    .append(DATE.format(o.moveInDate()))
                                    .append(o.phoneNumber() == null ? "" : " — ĐT " + o.phoneNumber())
                                    .append('\n'));
        }
        List<DebtResponse> debts =
                billing.debts(room.getRoomCode(), null, PageRequest.of(0, 100, Sort.by("dueDate")))
                        .stream()
                        .filter(d -> room.getId().equals(d.roomId()))
                        .toList();
        if (debts.isEmpty()) {
            out.append("• Không có hoá đơn còn nợ.");
        } else {
            BigDecimal total =
                    debts.stream()
                            .map(DebtResponse::outstandingAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            out.append("• Còn nợ ")
                    .append(money(total))
                    .append(" trên ")
                    .append(debts.size())
                    .append(" hoá đơn (sớm nhất hạn ")
                    .append(DATE.format(debts.get(0).dueDate()))
                    .append(").");
        }
        return out.toString().trim();
    }

    private String adminMonthlyDebt(YearMonth month) {
        String period = month.toString();
        List<DebtResponse> debts =
                billing.debts(null, null, PageRequest.of(0, 1000, Sort.by("dueDate"))).stream()
                        .filter(d -> period.equals(d.billingPeriod()))
                        .toList();
        BigDecimal total =
                debts.stream()
                        .map(DebtResponse::outstandingAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debts.isEmpty())
            return "Tháng " + period + " hiện không có khoản công nợ hóa đơn nào chưa thu.";
        StringBuilder out =
                new StringBuilder("Tháng ")
                        .append(period)
                        .append(" còn ")
                        .append(money(total))
                        .append(" chưa thu từ ")
                        .append(debts.size())
                        .append(" hóa đơn:\n");
        debts.forEach(
                d ->
                        out.append("• Phòng ")
                                .append(d.roomCode())
                                .append(" — ")
                                .append(d.payerName())
                                .append(" — ")
                                .append(d.invoiceCode())
                                .append(": ")
                                .append(money(d.outstandingAmount()))
                                .append(", hạn ")
                                .append(DATE.format(d.dueDate()))
                                .append('\n'));
        return out.toString().trim();
    }

    private String adminExpiringContracts(int days) {
        LocalDate today = LocalDate.now(ZONE), until = today.plusDays(days);
        List<ContractEntity> list =
                contracts.findByStatusInAndEndDateBetweenOrderByEndDateAsc(ACTIVE, today, until);
        if (list.isEmpty()) return "Không có hợp đồng nào hết hạn trong " + days + " ngày tới.";
        StringBuilder out =
                new StringBuilder("Có ")
                        .append(list.size())
                        .append(" hợp đồng hết hạn trong ")
                        .append(days)
                        .append(" ngày tới:\n");
        list.forEach(
                c ->
                        out.append("• Phòng ")
                                .append(c.getRoom().getRoomCode())
                                .append(" — ")
                                .append(c.getContractCode())
                                .append(" — ")
                                .append(c.getTenantUser().getFullName())
                                .append(" — hết hạn ")
                                .append(DATE.format(c.getEndDate()))
                                .append('\n'));
        return out.toString().trim();
    }

    private String adminOverview() {
        ReportDtos.Dashboard d = reports.dashboard(LocalDate.now(ZONE));
        return "Tổng quan hiện tại:\n"
                + "• Phòng: "
                + d.totalRooms()
                + " tổng, "
                + d.occupiedRooms()
                + " đang có người, "
                + d.vacantRooms()
                + " còn trống\n"
                + "• Hợp đồng: "
                + d.activeContracts()
                + " đang hiệu lực, "
                + d.expiringContracts()
                + " sắp hết hạn\n"
                + "• Công nợ: "
                + money(d.outstandingDebt())
                + " trên "
                + d.unpaidInvoices()
                + " hóa đơn, trong đó "
                + d.overdueInvoices()
                + " hóa đơn quá hạn\n"
                + "• Yêu cầu thuê chờ xử lý: "
                + d.pendingRentalRequests();
    }

    private String adminRooms() {
        List<ReportDtos.Count> rows = reports.rooms();
        if (rows.isEmpty()) return "Chưa có dữ liệu phòng trong hệ thống.";
        StringBuilder out = new StringBuilder("Tình trạng phòng hiện tại:\n");
        rows.forEach(
                row ->
                        out.append("• ")
                                .append(roomStatus(row.code()))
                                .append(": ")
                                .append(row.count())
                                .append(" phòng\n"));
        return out.toString().trim();
    }

    private String adminRevenue(LocalDate first, LocalDate today) {
        List<ReportDtos.Money> rows = reports.revenue(first, today).rows();
        BigDecimal total =
                rows.stream()
                        .map(ReportDtos.Money::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return "Doanh thu thực thu từ "
                + DATE.format(first)
                + " đến "
                + DATE.format(today)
                + " là "
                + money(total)
                + ".";
    }

    private String adminMaintenance() {
        List<ReportDtos.Count> rows = reports.maintenance();
        if (rows.isEmpty()) return "Chưa có yêu cầu bảo trì nào trong hệ thống.";
        StringBuilder out = new StringBuilder("Tình trạng yêu cầu bảo trì:\n");
        rows.forEach(
                row ->
                        out.append("• ")
                                .append(maintenanceStatus(row.code()))
                                .append(": ")
                                .append(row.count())
                                .append(" yêu cầu\n"));
        return out.toString().trim();
    }

    private String adminUtilities(YearMonth month) {
        String period = month.toString();
        List<ReportDtos.Money> rows = reports.utilities(period, period).rows();
        if (rows.isEmpty()) return "Tháng " + period + " chưa có dữ liệu tiêu thụ điện nước.";
        StringBuilder out =
                new StringBuilder("Tiêu thụ điện nước tháng ").append(period).append(":\n");
        rows.forEach(
                row ->
                        out.append("• ")
                                .append(row.code())
                                .append(": ")
                                .append(row.amount().stripTrailingZeros().toPlainString())
                                .append('\n'));
        return out.toString().trim();
    }

    private String tenantInvoices(Long userId) {
        List<DebtResponse> debts =
                billing.debts(
                                null,
                                userId,
                                PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "dueDate")))
                        .getContent();
        BigDecimal total =
                debts.stream()
                        .map(DebtResponse::outstandingAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debts.isEmpty()) return "Bạn hiện không có hóa đơn nào còn nợ.";
        StringBuilder out =
                new StringBuilder("Bạn còn ")
                        .append(money(total))
                        .append(" trên ")
                        .append(debts.size())
                        .append(" hóa đơn:\n");
        debts.forEach(
                d ->
                        out.append("• ")
                                .append(d.invoiceCode())
                                .append(" — phòng ")
                                .append(d.roomCode())
                                .append(": ")
                                .append(money(d.outstandingAmount()))
                                .append(", hạn ")
                                .append(DATE.format(d.dueDate()))
                                .append('\n'));
        return out.toString().trim();
    }

    private String tenantUtilities(Long userId) {
        List<InvoiceResponse> invoices =
                billing.searchForTenant(
                                userId,
                                PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "issueDate")))
                        .getContent();
        if (invoices.isEmpty()) return "Bạn chưa có hóa đơn điện nước nào trong hệ thống.";
        StringBuilder out = new StringBuilder("Điện, nước trên các hóa đơn gần đây của bạn:\n");
        for (InvoiceResponse invoice : invoices) {
            List<InvoiceItemResponse> utilities =
                    invoice.items().stream()
                            .filter(
                                    i ->
                                            i.itemType() == InvoiceItemType.ELECTRICITY
                                                    || i.itemType() == InvoiceItemType.WATER)
                            .toList();
            if (utilities.isEmpty()) continue;
            out.append("• Kỳ ")
                    .append(invoice.billingPeriod())
                    .append(" — phòng ")
                    .append(invoice.roomCode())
                    .append(": ");
            out.append(
                    utilities.stream()
                            .map(
                                    i ->
                                            label(i.itemType())
                                                    + " "
                                                    + i.quantity()
                                                            .stripTrailingZeros()
                                                            .toPlainString()
                                                    + " "
                                                    + i.unitName()
                                                    + " = "
                                                    + money(i.amount()))
                            .reduce((a, b) -> a + "; " + b)
                            .orElse(""));
            out.append('\n');
        }
        return out.length() == "Điện, nước trên các hóa đơn gần đây của bạn:\n".length()
                ? "Các hóa đơn gần đây của bạn chưa có dòng tiền điện hoặc nước."
                : out.toString().trim();
    }

    private String tenantContract(Long userId) {
        List<ContractEntity> list = contracts.findByTenantUser_IdAndStatusIn(userId, ACTIVE);
        if (list.isEmpty()) return "Bạn hiện không có hợp đồng đang hiệu lực.";
        StringBuilder out = new StringBuilder("Hợp đồng đang hiệu lực của bạn:\n");
        list.forEach(
                c ->
                        out.append("• ")
                                .append(c.getContractCode())
                                .append(" — phòng ")
                                .append(c.getRoom().getRoomCode())
                                .append(" — từ ")
                                .append(DATE.format(c.getStartDate()))
                                .append(" đến ")
                                .append(DATE.format(c.getEndDate()))
                                .append(" — tiền thuê ")
                                .append(money(c.getMonthlyRent()))
                                .append("/tháng\n"));
        return out.toString().trim();
    }

    private String tenantPaymentDue(Long userId) {
        List<DebtResponse> debts =
                billing.debts(null, userId, PageRequest.of(0, 50, Sort.by("dueDate"))).getContent();
        if (debts.isEmpty()) return "Bạn hiện không có khoản thanh toán nào đang chờ.";
        StringBuilder out = new StringBuilder("Các hạn thanh toán của bạn:\n");
        LocalDate today = LocalDate.now(ZONE);
        debts.forEach(
                d ->
                        out.append("• ")
                                .append(d.invoiceCode())
                                .append(": ")
                                .append(money(d.outstandingAmount()))
                                .append(" — hạn ")
                                .append(DATE.format(d.dueDate()))
                                .append(d.dueDate().isBefore(today) ? " (đã quá hạn)" : "")
                                .append('\n'));
        return out.toString().trim();
    }

    private String tenantPayments(Long userId) {
        var list =
                payments.searchTenant(
                                userId, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")))
                        .getContent();
        if (list.isEmpty()) return "Bạn chưa có giao dịch thanh toán nào trong hệ thống.";
        StringBuilder out = new StringBuilder("Các giao dịch thanh toán gần đây của bạn:\n");
        list.forEach(
                p ->
                        out.append("• ")
                                .append(p.transactionCode())
                                .append(": ")
                                .append(money(p.amount()))
                                .append(" — ")
                                .append(paymentStatus(p.status().name()))
                                .append(
                                        p.confirmedAt() == null
                                                ? ""
                                                : " — "
                                                        + p.confirmedAt()
                                                                .atZone(java.time.ZoneOffset.UTC)
                                                                .withZoneSameInstant(ZONE)
                                                                .format(
                                                                        DateTimeFormatter.ofPattern(
                                                                                "dd/MM/yyyy"
                                                                                    + " HH:mm")))
                                .append('\n'));
        return out.toString().trim();
    }

    private String tenantMaintenance(Long userId) {
        MaintenanceQuery query =
                new MaintenanceQuery(null, null, null, null, null, userId, null, null, null);
        List<MaintenanceResponse> list =
                maintenance
                        .searchForTenant(
                                query,
                                userId,
                                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .getContent();
        if (list.isEmpty()) return "Bạn chưa gửi yêu cầu bảo trì nào.";
        StringBuilder out = new StringBuilder("Các yêu cầu bảo trì gần đây của bạn:\n");
        list.forEach(
                item ->
                        out.append("• ")
                                .append(item.requestCode())
                                .append(" — phòng ")
                                .append(item.roomCode())
                                .append(" — ")
                                .append(maintenanceStatus(item.status().name()))
                                .append(" — ")
                                .append(item.title())
                                .append('\n'));
        return out.toString().trim();
    }

    private AppUserEntity current(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccessDeniedException("Authentication required");
        AppUserEntity user =
                users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                authentication.getName(), authentication.getName())
                        .orElseThrow(() -> new AccessDeniedException("Access denied"));
        if (!user.isActive() || user.getRole() == null)
            throw new AccessDeniedException("Access denied");
        return user;
    }

    private String requireAdmin(String role, java.util.function.Supplier<String> answer) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException("Access denied");
        return answer.get();
    }

    private String requireTenant(String role, java.util.function.Supplier<String> answer) {
        if (!"TENANT".equals(role)) throw new AccessDeniedException("Access denied");
        return answer.get();
    }

    private String unsupported(String role) {
        return "ADMIN".equals(role)
                ? "Tôi có thể tra cứu tổng quan, phòng, doanh thu, công nợ, điện nước, bảo trì và"
                      + " hợp đồng sắp hết hạn."
                : "Tôi có thể tra cứu hóa đơn, điện nước, hợp đồng, hạn thanh toán, giao dịch và"
                      + " yêu cầu bảo trì của bạn.";
    }

    private static String money(BigDecimal value) {
        return MONEY.format(value) + " đ";
    }

    private static String label(InvoiceItemType type) {
        return type == InvoiceItemType.ELECTRICITY ? "Điện" : "Nước";
    }

    private static String roomStatus(String value) {
        return switch (value) {
            case "VACANT" -> "Còn trống";
            case "OCCUPIED" -> "Đang có người";
            case "FULL" -> "Đã đủ người";
            case "MAINTENANCE" -> "Đang bảo trì";
            case "INACTIVE" -> "Ngừng hoạt động";
            default -> value;
        };
    }

    private static String maintenanceStatus(String value) {
        return switch (value) {
            case "OPEN" -> "Mới tiếp nhận";
            case "IN_PROGRESS" -> "Đang xử lý";
            case "WAITING" -> "Đang chờ";
            case "RESOLVED" -> "Đã xử lý";
            case "CLOSED" -> "Đã đóng";
            case "REJECTED" -> "Đã từ chối";
            default -> value;
        };
    }

    private static String paymentStatus(String value) {
        return switch (value) {
            case "SUCCEEDED" -> "Thành công";
            case "PENDING" -> "Đang chờ";
            case "FAILED" -> "Thất bại";
            case "CANCELLED" -> "Đã hủy";
            case "EXPIRED" -> "Hết hạn";
            case "MANUAL_REVIEW" -> "Chờ đối soát";
            default -> value;
        };
    }
}
