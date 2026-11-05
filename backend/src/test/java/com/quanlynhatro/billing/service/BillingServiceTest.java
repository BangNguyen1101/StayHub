package com.quanlynhatro.billing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.billing.api.*;
import com.quanlynhatro.billing.domain.InvoiceEntity;
import com.quanlynhatro.billing.domain.InvoiceItemEntity;
import com.quanlynhatro.billing.repository.*;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.domain.ContractOccupantEntity;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.utility.domain.FeeConfigEntity;
import com.quanlynhatro.utility.domain.ServiceEntity;
import com.quanlynhatro.utility.domain.UtilityReadingEntity;
import com.quanlynhatro.utility.repository.FeeConfigRepository;
import com.quanlynhatro.utility.repository.ServiceRepository;
import com.quanlynhatro.utility.repository.UtilityReadingRepository;

import com.quanlynhatro.billing.api.InvoiceStatus;
import com.quanlynhatro.notification.domain.NotificationEntity;
import com.quanlynhatro.notification.repository.NotificationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingServiceTest {
    @Mock InvoiceRepository invoices;
    @Mock InvoiceItemRepository items;
    @Mock ContractRepository contracts;
    @Mock ContractOccupantRepository occupants;
    @Mock UtilityReadingRepository readings;
    @Mock FeeConfigRepository fees;
    @Mock ServiceRepository services;
    @Mock AppUserRepository users;
    @Mock PaymentRepository payments;

    @Test
    void blocksAutomaticBillingWhenRoomHasMultipleActiveContracts() {
        BillingServiceImpl service = legacyService();
        ContractEntity contract = mock(ContractEntity.class);
        RoomEntity room = mock(RoomEntity.class);
        when(contracts.findByIdForUpdate(1L)).thenReturn(Optional.of(contract));
        when(users.findById(9L)).thenReturn(Optional.of(mock(AppUserEntity.class)));
        when(contract.getStatus()).thenReturn(ContractStatus.ACTIVE);
        when(contract.getStartDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(contract.getEndDate()).thenReturn(LocalDate.of(2026, 12, 31));
        when(contract.getRoom()).thenReturn(room);
        when(room.getId()).thenReturn(5L);
        when(contracts.findByRoom_IdAndStatusIn(eq(5L), any()))
                .thenReturn(List.of(contract, mock(ContractEntity.class)));
        assertThrows(
                InvoiceConflictException.class,
                () -> service.createDraft(request(null, null, null), 9L));
        verify(invoices, never()).saveAndFlush(any());
    }

    @Test
    void issuedInvoiceUsesPessimisticRepositoryLookup() {
        BillingServiceImpl service = legacyService();
        InvoiceEntity invoice = mock(InvoiceEntity.class);
        when(invoices.findByIdForUpdate(7L)).thenReturn(Optional.of(invoice));
        when(users.findById(9L)).thenReturn(Optional.of(mock(AppUserEntity.class)));
        when(invoice.getId()).thenReturn(7L);
        when(invoice.getStatus()).thenReturn(InvoiceStatus.DRAFT);
        stubInvoiceAmounts(invoice);
        when(items.findByInvoice_IdOrderByIdAsc(7L)).thenReturn(List.of());
        service.issue(7L, 9L);
        verify(invoices).findByIdForUpdate(7L);
        verify(invoice).issue(any());
    }

    @Test
    void tenantCannotOpenDraftInvoice() {
        BillingServiceImpl service = legacyService();
        InvoiceEntity invoice = mock(InvoiceEntity.class);
        AppUserEntity payer = mock(AppUserEntity.class);
        when(invoices.findById(6L)).thenReturn(Optional.of(invoice));
        when(invoice.getPayerUser()).thenReturn(payer);
        when(payer.getId()).thenReturn(12L);
        when(invoice.getStatus()).thenReturn(InvoiceStatus.DRAFT);

        assertThrows(InvoiceNotFoundException.class, () -> service.getTenant(6L, 12L));
    }

    @Test
    void issuingDraftsIssuesOnlyDraftsAndNotifiesTenants() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        BillingServiceImpl service =
                new BillingServiceImpl(
                        invoices, items, contracts, occupants, readings, fees, services, users,
                        payments, notifications);
        InvoiceEntity draft = issuableInvoice(6L, InvoiceStatus.DRAFT);
        InvoiceEntity alreadyIssued = issuableInvoice(7L, InvoiceStatus.UNPAID);
        when(users.findById(9L)).thenReturn(Optional.of(mock(AppUserEntity.class)));
        when(invoices.findDraftIds("2026-11")).thenReturn(List.of(6L, 7L));
        when(invoices.findByIdForUpdate(6L)).thenReturn(Optional.of(draft));
        when(invoices.findByIdForUpdate(7L)).thenReturn(Optional.of(alreadyIssued));

        assertEquals(1, service.issueDrafts("2026-11", 9L));
        verify(draft).issue(any());
        verify(alreadyIssued, never()).issue(any());
        verify(notifications).save(any(NotificationEntity.class));
        assertThrows(InvoiceConflictException.class, () -> service.issueDrafts("11/2026", 9L));
    }

    @Test
    void issuingNonDraftInvoiceIsAConflict() {
        BillingServiceImpl service = legacyService();
        InvoiceEntity invoice = issuableInvoice(7L, InvoiceStatus.UNPAID);
        when(invoices.findByIdForUpdate(7L)).thenReturn(Optional.of(invoice));
        when(users.findById(9L)).thenReturn(Optional.of(mock(AppUserEntity.class)));

        assertThrows(InvoiceConflictException.class, () -> service.issue(7L, 9L));
        verify(invoice, never()).issue(any());
    }

    private InvoiceEntity issuableInvoice(Long id, InvoiceStatus status) {
        InvoiceEntity invoice = mock(InvoiceEntity.class);
        RoomEntity room = mock(RoomEntity.class);
        when(invoice.getId()).thenReturn(id);
        when(invoice.getStatus()).thenReturn(status);
        when(invoice.getInvoiceCode()).thenReturn("INV-" + id);
        when(invoice.getBillingPeriod()).thenReturn("2026-11");
        when(invoice.getRoom()).thenReturn(room);
        when(room.getRoomCode()).thenReturn("B101");
        when(invoice.getDueDate()).thenReturn(LocalDate.of(2026, 11, 10));
        when(invoice.getPayerUser()).thenReturn(mock(AppUserEntity.class));
        return invoice;
    }

    @Test
    void debtUsesSucceededPaymentsInsteadOfStaleInvoicePaidAmount() {
        BillingServiceImpl service = serviceWithPayments();
        InvoiceEntity invoice = mock(InvoiceEntity.class);
        ContractEntity contract = mock(ContractEntity.class);
        RoomEntity room = mock(RoomEntity.class);
        when(invoice.getId()).thenReturn(20L);
        when(invoice.getInvoiceCode()).thenReturn("INV-202601-1");
        when(invoice.getContract()).thenReturn(contract);
        when(invoice.getRoom()).thenReturn(room);
        when(invoice.getBillingPeriod()).thenReturn("2026-01");
        when(invoice.getDueDate()).thenReturn(LocalDate.now().plusDays(2));
        when(invoice.getTotalAmount()).thenReturn(new BigDecimal("100.00"));
        AppUserEntity payer = mock(AppUserEntity.class);
        when(invoice.getPayerUser()).thenReturn(payer);
        when(payer.getId()).thenReturn(2L);
        when(contract.getId()).thenReturn(3L);
        when(room.getId()).thenReturn(4L);
        when(invoices.searchOutstanding(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(invoice)));
        when(payments.existsByInvoice_IdAndStatus(20L, PaymentStatus.SUCCEEDED)).thenReturn(true);
        when(payments.sumAmount(20L, PaymentStatus.SUCCEEDED)).thenReturn(new BigDecimal("40.00"));
        DebtResponse debt = service.debts(null, null, PageRequest.of(0, 10)).getContent().get(0);
        assertEquals(new BigDecimal("40.00"), debt.paidAmount());
        assertEquals(new BigDecimal("60.00"), debt.outstandingAmount());
        assertEquals(InvoiceStatus.PARTIALLY_PAID, debt.status());
    }

    @Test
    void createsSnapshotAmountsForRentUtilityAndService() {
        BillingServiceImpl service = legacyService();
        ContractEntity contract = mock(ContractEntity.class);
        RoomEntity room = mock(RoomEntity.class);
        AppUserEntity tenant = mock(AppUserEntity.class);
        UtilityReadingEntity reading = mock(UtilityReadingEntity.class);
        FeeConfigEntity fee = mock(FeeConfigEntity.class);
        ServiceEntity extra = mock(ServiceEntity.class);
        ContractOccupantEntity occupant = mock(ContractOccupantEntity.class);
        when(contracts.findByIdForUpdate(1L)).thenReturn(Optional.of(contract));
        when(users.findById(9L)).thenReturn(Optional.of(mock(AppUserEntity.class)));
        when(contract.getId()).thenReturn(1L);
        when(contract.getStatus()).thenReturn(ContractStatus.ACTIVE);
        when(contract.getStartDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(contract.getEndDate()).thenReturn(LocalDate.of(2026, 12, 31));
        when(contract.getRoom()).thenReturn(room);
        when(contract.getTenantUser()).thenReturn(tenant);
        when(contract.getMonthlyRent()).thenReturn(new BigDecimal("3000000.00"));
        when(room.getId()).thenReturn(5L);
        when(contracts.findByRoom_IdAndStatusIn(eq(5L), any())).thenReturn(List.of(contract));
        when(occupants.findCurrentByContractId(1L, LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(occupant));
        when(occupant.getMoveInDate()).thenReturn(LocalDate.of(2025, 12, 1));
        when(occupant.getMoveOutDate()).thenReturn(null);
        when(invoices.existsByContract_IdAndBillingPeriod(1L, "2026-01")).thenReturn(false);
        when(invoices.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(items.findByInvoice_IdOrderByIdAsc(any())).thenReturn(List.of());
        when(readings.findByRoom_IdAndUtilityTypeAndBillingPeriod(5L, "ELECTRICITY", "2026-01"))
                .thenReturn(Optional.of(reading));
        when(reading.getId()).thenReturn(11L);
        when(reading.isLocked()).thenReturn(true);
        when(reading.getConsumption()).thenReturn(new BigDecimal("2.500"));
        when(items.existsByUtilityReading_Id(11L)).thenReturn(false);
        when(fees.findEffective("ELEC", LocalDate.of(2026, 1, 1))).thenReturn(Optional.of(fee));
        when(fee.getUnitPrice()).thenReturn(new BigDecimal("3500.00"));
        when(fee.getUnitName()).thenReturn("kWh");
        when(services.findById(7L)).thenReturn(Optional.of(extra));
        when(extra.isActive()).thenReturn(true);
        when(extra.getPricingMode()).thenReturn("PER_USAGE");
        when(extra.getServiceName()).thenReturn("Internet");
        when(extra.getDefaultUnitPrice()).thenReturn(new BigDecimal("100.00"));
        InvoiceResponse response =
                service.createDraft(
                        request(
                                "ELEC",
                                null,
                                List.of(new InvoiceServiceItemRequest(7L, new BigDecimal("2")))),
                        9L);
        assertEquals(new BigDecimal("3000000.00"), response.rentAmount());
        assertEquals(new BigDecimal("8750.00"), response.utilityAmount());
        assertEquals(new BigDecimal("200.00"), response.serviceAmount());
        assertEquals(new BigDecimal("3008950.00"), response.totalAmount());
        ArgumentCaptor<InvoiceItemEntity> captured =
                ArgumentCaptor.forClass(InvoiceItemEntity.class);
        verify(items, atLeastOnce()).save(captured.capture());
        assertTrue(
                captured.getAllValues().stream()
                        .anyMatch(
                                item ->
                                        item.getItemType() == InvoiceItemType.ELECTRICITY
                                                && new BigDecimal("3500.00")
                                                                .compareTo(item.getUnitPrice())
                                                        == 0));
    }

    @Test
    void manualUsageUsesFixedPricesIgnoresClientAmountAndAddsNoImplicitServices() {
        BillingServiceImpl service = manualUsageService();
        InvoiceCreateRequest request =
                new InvoiceCreateRequest(
                        1L,
                        null,
                        "2026-01",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 5),
                        null,
                        null,
                        new BigDecimal("10"),
                        new BigDecimal("1"),
                        new BigDecimal("2"),
                        new BigDecimal("1"),
                        null,
                        null);
        InvoiceResponse response = service.createDraft(request, 9L);
        // 10 kWh x 3,500 + 2 m3 x 30,000 = 95,000; the client-sent amounts (1 VND) are ignored.
        assertEquals(new BigDecimal("95000.00"), response.utilityAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.serviceAmount()));
        ArgumentCaptor<InvoiceItemEntity> captured =
                ArgumentCaptor.forClass(InvoiceItemEntity.class);
        verify(items, atLeastOnce()).save(captured.capture());
        assertTrue(
                captured.getAllValues().stream()
                        .noneMatch(item -> item.getItemType() == InvoiceItemType.SERVICE));
    }

    @Test
    void manualAmountWithoutUsageIsRejected() {
        BillingServiceImpl service = manualUsageService();
        InvoiceCreateRequest request =
                new InvoiceCreateRequest(
                        1L,
                        null,
                        "2026-01",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 5),
                        null,
                        null,
                        null,
                        new BigDecimal("50000"),
                        null,
                        null,
                        null,
                        null);
        assertThrows(InvoiceConflictException.class, () -> service.createDraft(request, 9L));
    }

    private BillingServiceImpl manualUsageService() {
        BillingServiceImpl service = legacyService();
        ContractEntity contract = mock(ContractEntity.class);
        RoomEntity room = mock(RoomEntity.class);
        ContractOccupantEntity occupant = mock(ContractOccupantEntity.class);
        when(contracts.findByIdForUpdate(1L)).thenReturn(Optional.of(contract));
        when(users.findById(9L)).thenReturn(Optional.of(mock(AppUserEntity.class)));
        when(contract.getId()).thenReturn(1L);
        when(contract.getStatus()).thenReturn(ContractStatus.ACTIVE);
        when(contract.getRoom()).thenReturn(room);
        when(contract.getTenantUser()).thenReturn(mock(AppUserEntity.class));
        when(contract.getMonthlyRent()).thenReturn(new BigDecimal("3000000.00"));
        when(room.getId()).thenReturn(5L);
        when(occupants.findCurrentByContractId(1L, LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(occupant));
        when(invoices.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(items.findByInvoice_IdOrderByIdAsc(any())).thenReturn(List.of());
        return service;
    }

    @Test
    void blocksUnlockedUtilityReading() {
        assertThrows(
                InvoiceConflictException.class,
                () ->
                        preparedUtilityService(false, false, true)
                                .createDraft(request("ELEC", null, null), 9L));
    }

    @Test
    void blocksUtilityReadingAlreadyUsedByInvoice() {
        assertThrows(
                InvoiceConflictException.class,
                () ->
                        preparedUtilityService(true, true, true)
                                .createDraft(request("ELEC", null, null), 9L));
    }

    @Test
    void blocksMissingEffectiveFee() {
        assertThrows(
                InvoiceConflictException.class,
                () ->
                        preparedUtilityService(true, false, false)
                                .createDraft(request("ELEC", null, null), 9L));
    }

    private BillingServiceImpl legacyService() {
        return new BillingServiceImpl(
                invoices, items, contracts, occupants, readings, fees, services, users);
    }

    private BillingServiceImpl serviceWithPayments() {
        return new BillingServiceImpl(
                invoices, items, contracts, occupants, readings, fees, services, users, payments);
    }

    private InvoiceCreateRequest request(
            String electricityFee, String waterFee, List<InvoiceServiceItemRequest> serviceItems) {
        return new InvoiceCreateRequest(
                1L,
                "2026-01",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 5),
                electricityFee,
                waterFee,
                serviceItems,
                null);
    }

    private void stubInvoiceAmounts(InvoiceEntity invoice) {
        when(invoice.getContract()).thenReturn(mock(ContractEntity.class));
        when(invoice.getRoom()).thenReturn(mock(RoomEntity.class));
        when(invoice.getPayerUser()).thenReturn(mock(AppUserEntity.class));
        when(invoice.getRentAmount()).thenReturn(BigDecimal.ZERO);
        when(invoice.getUtilityAmount()).thenReturn(BigDecimal.ZERO);
        when(invoice.getServiceAmount()).thenReturn(BigDecimal.ZERO);
        when(invoice.getAdjustmentAmount()).thenReturn(BigDecimal.ZERO);
        when(invoice.getTotalAmount()).thenReturn(BigDecimal.ZERO);
        when(invoice.getPaidAmount()).thenReturn(BigDecimal.ZERO);
    }

    private BillingServiceImpl preparedUtilityService(
            boolean locked, boolean alreadyUsed, boolean feeExists) {
        BillingServiceImpl service = legacyService();
        ContractEntity contract = mock(ContractEntity.class);
        RoomEntity room = mock(RoomEntity.class);
        UtilityReadingEntity reading = mock(UtilityReadingEntity.class);
        ContractOccupantEntity occupant = mock(ContractOccupantEntity.class);
        when(contracts.findByIdForUpdate(1L)).thenReturn(Optional.of(contract));
        when(users.findById(9L)).thenReturn(Optional.of(mock(AppUserEntity.class)));
        when(contract.getId()).thenReturn(1L);
        when(contract.getStatus()).thenReturn(ContractStatus.ACTIVE);
        when(contract.getStartDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(contract.getEndDate()).thenReturn(LocalDate.of(2026, 12, 31));
        when(contract.getRoom()).thenReturn(room);
        when(contract.getTenantUser()).thenReturn(mock(AppUserEntity.class));
        when(contract.getMonthlyRent()).thenReturn(BigDecimal.TEN);
        when(room.getId()).thenReturn(5L);
        when(contracts.findByRoom_IdAndStatusIn(eq(5L), any())).thenReturn(List.of(contract));
        when(occupants.findCurrentByContractId(1L, LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(occupant));
        when(occupant.getMoveInDate()).thenReturn(LocalDate.of(2025, 12, 1));
        when(occupant.getMoveOutDate()).thenReturn(null);
        when(invoices.existsByContract_IdAndBillingPeriod(1L, "2026-01")).thenReturn(false);
        when(invoices.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(items.findByInvoice_IdOrderByIdAsc(any())).thenReturn(List.of());
        when(readings.findByRoom_IdAndUtilityTypeAndBillingPeriod(5L, "ELECTRICITY", "2026-01"))
                .thenReturn(Optional.of(reading));
        when(reading.getId()).thenReturn(11L);
        when(reading.isLocked()).thenReturn(locked);
        when(reading.getConsumption()).thenReturn(BigDecimal.ONE);
        when(items.existsByUtilityReading_Id(11L)).thenReturn(alreadyUsed);
        FeeConfigEntity fee = mock(FeeConfigEntity.class);
        when(fee.getUnitPrice()).thenReturn(BigDecimal.ONE);
        when(fee.getUnitName()).thenReturn("kWh");
        when(fees.findEffective("ELEC", LocalDate.of(2026, 1, 1)))
                .thenReturn(feeExists ? Optional.of(fee) : Optional.empty());
        return service;
    }
}
