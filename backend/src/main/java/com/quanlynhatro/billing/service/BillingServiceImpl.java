package com.quanlynhatro.billing.service;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.billing.api.*;
import com.quanlynhatro.billing.domain.*;
import com.quanlynhatro.billing.repository.*;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.domain.ContractOccupantEntity;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.notification.domain.NotificationEntity;
import com.quanlynhatro.notification.repository.NotificationRepository;
import com.quanlynhatro.utility.domain.FeeConfigEntity;
import com.quanlynhatro.utility.domain.ServiceEntity;
import com.quanlynhatro.utility.domain.UtilityReadingEntity;
import com.quanlynhatro.utility.repository.FeeConfigRepository;
import com.quanlynhatro.utility.repository.ServiceRepository;
import com.quanlynhatro.utility.repository.UtilityReadingRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class BillingServiceImpl implements BillingService {
    private static final Set<ContractStatus> ACTIVE =
            Set.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING);
    private final InvoiceRepository invoices;
    private final InvoiceItemRepository items;
    private final ContractRepository contracts;
    private final ContractOccupantRepository occupants;
    private final UtilityReadingRepository readings;
    private final FeeConfigRepository fees;
    private final ServiceRepository services;
    private final AppUserRepository users;
    private final PaymentRepository payments;
    private final NotificationRepository notifications;

    public BillingServiceImpl(
            InvoiceRepository invoices,
            InvoiceItemRepository items,
            ContractRepository contracts,
            ContractOccupantRepository occupants,
            UtilityReadingRepository readings,
            FeeConfigRepository fees,
            ServiceRepository services,
            AppUserRepository users) {
        this(invoices, items, contracts, occupants, readings, fees, services, users, null, null);
    }

    public BillingServiceImpl(
            InvoiceRepository invoices,
            InvoiceItemRepository items,
            ContractRepository contracts,
            ContractOccupantRepository occupants,
            UtilityReadingRepository readings,
            FeeConfigRepository fees,
            ServiceRepository services,
            AppUserRepository users,
            PaymentRepository payments) {
        this(
                invoices, items, contracts, occupants, readings, fees, services, users, payments,
                null);
    }

    @Autowired
    public BillingServiceImpl(
            InvoiceRepository invoices,
            InvoiceItemRepository items,
            ContractRepository contracts,
            ContractOccupantRepository occupants,
            UtilityReadingRepository readings,
            FeeConfigRepository fees,
            ServiceRepository services,
            AppUserRepository users,
            PaymentRepository payments,
            NotificationRepository notifications) {
        this.invoices = invoices;
        this.items = items;
        this.contracts = contracts;
        this.occupants = occupants;
        this.readings = readings;
        this.fees = fees;
        this.services = services;
        this.users = users;
        this.payments = payments;
        this.notifications = notifications;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InvoiceResponse createDraft(InvoiceCreateRequest r, Long adminId) {
        ContractEntity c =
                contracts
                        .findByIdForUpdate(r.contractId())
                        .orElseThrow(
                                () ->
                                        new InvoiceConflictException(
                                                "Contract not found or cannot be billed: "
                                                        + r.contractId()));
        AppUserEntity admin =
                users.findById(adminId).orElseThrow(InvoiceAccessDeniedException::new);
        YearMonth ym = YearMonth.parse(r.billingPeriod());
        LocalDate first = ym.atDay(1), last = ym.atEndOfMonth();
        if (!ACTIVE.contains(c.getStatus()))
            throw new InvoiceConflictException("Contract is not active");
        if (r.dueDate().isBefore(r.issueDate()))
            throw new InvoiceConflictException("Due date cannot be before issue date");
        if (invoices.existsByContract_IdAndBillingPeriodAndStatusNot(
                c.getId(), r.billingPeriod(), InvoiceStatus.CANCELLED))
            throw new InvoiceConflictException("Invoice already exists for contract and period");

        List<ContractOccupantEntity> current = occupants.findCurrentByContractId(c.getId(), last);
        if (current.isEmpty())
            throw new InvoiceConflictException("Contract has no occupant for the billing period");

        AppUserEntity payer = c.getTenantUser();
        if (r.payerUserId() != null) {
            payer =
                    occupants.findCurrentByContractId(c.getId(), LocalDate.now()).stream()
                            .filter(
                                    o ->
                                            o.getUser() != null
                                                    && Objects.equals(
                                                            o.getUser().getId(), r.payerUserId()))
                            .map(ContractOccupantEntity::getUser)
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new InvoiceConflictException(
                                                    "Selected payer is not an occupant of this"
                                                        + " contract"));
        }
        InvoiceEntity invoice =
                InvoiceEntity.draft(
                        generateCode(r.billingPeriod()),
                        c,
                        c.getRoom(),
                        payer,
                        r.billingPeriod(),
                        r.issueDate(),
                        r.dueDate(),
                        r.note(),
                        admin);
        invoices.saveAndFlush(invoice);
        // The tenant is notified when the draft is issued, not now: drafts are not visible to them.
        BigDecimal rent = c.getMonthlyRent(), utility = BigDecimal.ZERO, service = BigDecimal.ZERO;
        items.save(
                InvoiceItemEntity.create(
                        invoice,
                        InvoiceItemType.RENT,
                        "Rent",
                        BigDecimal.ONE,
                        "MONTH",
                        rent,
                        null,
                        null,
                        null));
        utility =
                manualOrReading(
                        invoice,
                        c,
                        r.electricityUsage(),
                        r.electricityAmount(),
                        r.electricityFeeCode(),
                        "ELECTRICITY",
                        first);
        utility =
                utility.add(
                        manualOrReading(
                                invoice,
                                c,
                                r.waterUsage(),
                                r.waterAmount(),
                                r.waterFeeCode(),
                                "WATER",
                                first));
        if (r.services() != null)
            for (InvoiceServiceItemRequest sr : r.services()) {
                ServiceEntity s =
                        services.findById(sr.serviceId())
                                .orElseThrow(
                                        () ->
                                                new InvoiceConflictException(
                                                        "Service not found: " + sr.serviceId()));
                if (!s.isActive())
                    throw new InvoiceConflictException("Service is inactive: " + sr.serviceId());
                BigDecimal q = serviceQuantity(s, sr.quantity(), current.size());
                BigDecimal amount =
                        s.getDefaultUnitPrice().multiply(q).setScale(2, RoundingMode.HALF_UP);
                items.save(
                        InvoiceItemEntity.create(
                                invoice,
                                InvoiceItemType.SERVICE,
                                s.getServiceName(),
                                q,
                                s.getPricingMode(),
                                s.getDefaultUnitPrice(),
                                null,
                                null,
                                s));
                service = service.add(amount);
            }
        invoice.setAmounts(rent, utility, service, BigDecimal.ZERO);
        return toResponse(invoice);
    }

    /** Fixed unit price for electricity entered manually on the invoice form (VND per kWh). */
    static final BigDecimal MANUAL_ELECTRICITY_UNIT_PRICE = new BigDecimal("3500.00");

    /** Fixed unit price for water entered manually on the invoice form (VND per m3). */
    static final BigDecimal MANUAL_WATER_UNIT_PRICE = new BigDecimal("30000.00");

    private BigDecimal manualOrReading(
            InvoiceEntity invoice,
            ContractEntity c,
            BigDecimal usage,
            BigDecimal amount,
            String feeCode,
            String type,
            LocalDate at) {
        if (usage == null && amount != null)
            throw new InvoiceConflictException(
                    type + " usage is required; the amount is calculated by the server");
        if (usage != null) {
            if (usage.signum() < 0)
                throw new InvoiceConflictException("Utility usage cannot be negative");
            boolean electricity = type.equals("ELECTRICITY");
            BigDecimal unitPrice =
                    electricity ? MANUAL_ELECTRICITY_UNIT_PRICE : MANUAL_WATER_UNIT_PRICE;
            // The amount sent by the client is ignored: the server always computes usage x fixed
            // unit price.
            BigDecimal total = usage.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            items.save(
                    InvoiceItemEntity.create(
                            invoice,
                            electricity ? InvoiceItemType.ELECTRICITY : InvoiceItemType.WATER,
                            type,
                            usage,
                            electricity ? "kWh" : "m3",
                            unitPrice,
                            null,
                            null,
                            null));
            return total;
        }
        return addUtility(invoice, c, feeCode, type, at);
    }

    private BigDecimal addUtility(
            InvoiceEntity invoice, ContractEntity c, String feeCode, String type, LocalDate at) {
        UtilityReadingEntity reading =
                readings.findByRoom_IdAndUtilityTypeAndBillingPeriod(
                                c.getRoom().getId(), type, invoice.getBillingPeriod())
                        .orElse(null);
        if (reading == null) {
            if (feeCode != null && !feeCode.isBlank())
                throw new InvoiceConflictException("No " + type + " reading for billing period");
            return BigDecimal.ZERO;
        }
        if (!reading.isLocked())
            throw new InvoiceConflictException(type + " reading must be locked before invoicing");
        if (items.existsByUtilityReading_Id(reading.getId()))
            throw new InvoiceConflictException("Utility reading is already used by an invoice");
        if (feeCode == null || feeCode.isBlank())
            throw new InvoiceConflictException("Fee code is required for " + type);
        FeeConfigEntity fee =
                fees.findEffective(feeCode, at)
                        .orElseThrow(
                                () ->
                                        new InvoiceConflictException(
                                                "No effective fee config: " + feeCode));
        BigDecimal q = reading.getConsumption(),
                amount = fee.getUnitPrice().multiply(q).setScale(2, RoundingMode.HALF_UP);
        items.save(
                InvoiceItemEntity.create(
                        invoice,
                        type.equals("ELECTRICITY")
                                ? InvoiceItemType.ELECTRICITY
                                : InvoiceItemType.WATER,
                        type,
                        q,
                        fee.getUnitName(),
                        fee.getUnitPrice(),
                        fee,
                        reading,
                        null));
        return amount;
    }

    private BigDecimal serviceQuantity(ServiceEntity s, BigDecimal supplied, int occupants) {
        return switch (s.getPricingMode()) {
            case "PER_ROOM" -> BigDecimal.ONE;
            case "PER_PERSON" -> BigDecimal.valueOf(occupants);
            case "PER_USAGE" -> supplied;
            default ->
                    throw new InvoiceConflictException(
                            "Unsupported service pricing mode: " + s.getPricingMode());
        };
    }

    private LocalDate effectiveEnd(ContractEntity c) {
        return c.getActualEndDate() == null ? c.getEndDate() : c.getActualEndDate();
    }

    private String generateCode(String period) {
        return "INV-"
                + period.replace("-", "")
                + "-"
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8)
                        .toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InvoiceResponse issue(Long id, Long adminId) {
        InvoiceEntity i =
                invoices.findByIdForUpdate(id).orElseThrow(() -> new InvoiceNotFoundException(id));
        users.findById(adminId).orElseThrow(InvoiceAccessDeniedException::new);
        if (i.getStatus() != InvoiceStatus.DRAFT)
            throw new InvoiceConflictException("Chỉ phát hành được hoá đơn đang ở trạng thái nháp");
        i.issue(LocalDateTime.now());
        notifyIssued(i);
        return toResponse(i);
    }

    @Override
    @Transactional
    public int issueDrafts(String period, Long adminId) {
        users.findById(adminId).orElseThrow(InvoiceAccessDeniedException::new);
        String normalized = period == null || period.isBlank() ? null : period.trim();
        if (normalized != null && !normalized.matches("\\d{4}-(0[1-9]|1[0-2])"))
            throw new InvoiceConflictException("Kỳ hoá đơn phải có dạng yyyy-MM");
        int issued = 0;
        for (Long id : invoices.findDraftIds(normalized)) {
            InvoiceEntity i = invoices.findByIdForUpdate(id).orElse(null);
            // Skip invoices issued, cancelled or deleted concurrently since the id lookup.
            if (i == null || i.getStatus() != InvoiceStatus.DRAFT) continue;
            i.issue(LocalDateTime.now());
            notifyIssued(i);
            issued++;
        }
        return issued;
    }

    private void notifyIssued(InvoiceEntity i) {
        if (notifications == null) return;
        notifications.save(
                NotificationEntity.create(
                        i.getPayerUser(),
                        "INVOICE_DUE",
                        "Hóa đơn mới cần thanh toán",
                        "Hóa đơn "
                                + i.getInvoiceCode()
                                + " kỳ "
                                + i.getBillingPeriod()
                                + " của phòng "
                                + i.getRoom().getRoomCode()
                                + " đến hạn ngày "
                                + i.getDueDate()
                                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                + ". Vui lòng mở hóa đơn để thanh toán.",
                        "INVOICE",
                        i.getId()));
    }

    @Override
    @Transactional
    public void delete(Long id, Long adminId) {
        InvoiceEntity i =
                invoices.findByIdForUpdate(id).orElseThrow(() -> new InvoiceNotFoundException(id));
        users.findById(adminId).orElseThrow(InvoiceAccessDeniedException::new);
        if (i.getStatus() != InvoiceStatus.DRAFT && i.getStatus() != InvoiceStatus.CANCELLED)
            throw new InvoiceConflictException("Only draft or cancelled invoices can be deleted");
        if (payments != null && payments.existsByInvoice_IdAndStatus(id, PaymentStatus.SUCCEEDED))
            throw new InvoiceConflictException("Paid invoices cannot be deleted");
        items.deleteByInvoice_Id(id);
        invoices.delete(i);
    }

    @Override
    @Transactional
    public InvoiceResponse cancel(Long id, Long adminId) {
        InvoiceEntity i =
                invoices.findByIdForUpdate(id).orElseThrow(() -> new InvoiceNotFoundException(id));
        users.findById(adminId).orElseThrow(InvoiceAccessDeniedException::new);
        i.cancel();
        return toResponse(i);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getAdmin(Long id) {
        return toResponse(
                invoices.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getTenant(Long id, Long tenantId) {
        InvoiceEntity i = invoices.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id));
        if (!Objects.equals(i.getPayerUser().getId(), tenantId))
            throw new InvoiceAccessDeniedException();
        if (i.getStatus() == InvoiceStatus.DRAFT) throw new InvoiceNotFoundException(id);
        return toResponse(i);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> searchForTenant(Long tenantId, Pageable pageable) {
        return invoices.searchIssuedForPayer(tenantId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> search(
            String keyword,
            String period,
            InvoiceStatus status,
            Long contractId,
            Long payerId,
            Pageable pageable) {
        return invoices.search(keyword, period, status, contractId, payerId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DebtResponse> debts(String keyword, Long payerId, Pageable pageable) {
        return invoices.searchOutstanding(keyword, payerId, pageable).map(this::toDebt);
    }

    private BigDecimal paidAmount(InvoiceEntity i) {
        if (payments == null || i.getId() == null)
            return Optional.ofNullable(i.getPaidAmount()).orElse(BigDecimal.ZERO);
        if (!payments.existsByInvoice_IdAndStatus(i.getId(), PaymentStatus.SUCCEEDED))
            return Optional.ofNullable(i.getPaidAmount()).orElse(BigDecimal.ZERO);
        return Optional.ofNullable(payments.sumAmount(i.getId(), PaymentStatus.SUCCEEDED))
                .orElse(BigDecimal.ZERO);
    }

    private DebtResponse toDebt(InvoiceEntity i) {
        BigDecimal paid = paidAmount(i), out = i.getTotalAmount().subtract(paid);
        InvoiceStatus s =
                out.signum() == 0
                        ? InvoiceStatus.PAID
                        : (i.getDueDate().isBefore(LocalDate.now())
                                ? InvoiceStatus.OVERDUE
                                : (paid.signum() > 0
                                        ? InvoiceStatus.PARTIALLY_PAID
                                        : InvoiceStatus.UNPAID));
        long days =
                s == InvoiceStatus.OVERDUE
                        ? java.time.temporal.ChronoUnit.DAYS.between(
                                i.getDueDate(), LocalDate.now())
                        : 0;
        return new DebtResponse(
                i.getId(),
                i.getInvoiceCode(),
                i.getContract().getId(),
                i.getRoom().getId(),
                i.getRoom().getRoomCode(),
                i.getPayerUser().getId(),
                i.getPayerUser().getFullName(),
                i.getBillingPeriod(),
                i.getDueDate(),
                i.getTotalAmount(),
                paid,
                out,
                s,
                days);
    }

    private InvoiceResponse toResponse(InvoiceEntity i) {
        List<InvoiceItemResponse> lines =
                items.findByInvoice_IdOrderByIdAsc(i.getId()).stream()
                        .map(
                                x ->
                                        new InvoiceItemResponse(
                                                x.getId(),
                                                x.getItemType(),
                                                x.getDescription(),
                                                x.getQuantity(),
                                                x.getUnitName(),
                                                x.getUnitPrice(),
                                                x.getAmount(),
                                                x.getFeeConfig() == null
                                                        ? null
                                                        : x.getFeeConfig().getId(),
                                                x.getUtilityReading() == null
                                                        ? null
                                                        : x.getUtilityReading().getId(),
                                                x.getService() == null
                                                        ? null
                                                        : x.getService().getId()))
                        .toList();
        BigDecimal paid = paidAmount(i);
        return new InvoiceResponse(
                i.getId(),
                i.getInvoiceCode(),
                i.getContract().getId(),
                i.getRoom().getId(),
                i.getRoom().getRoomCode(),
                i.getPayerUser().getId(),
                i.getBillingPeriod(),
                i.getIssueDate(),
                i.getDueDate(),
                i.getRentAmount(),
                i.getUtilityAmount(),
                i.getServiceAmount(),
                i.getAdjustmentAmount(),
                i.getTotalAmount(),
                paid,
                i.getTotalAmount().subtract(paid),
                i.getStatus(),
                i.getNote(),
                i.getIssuedAt(),
                lines);
    }
}
