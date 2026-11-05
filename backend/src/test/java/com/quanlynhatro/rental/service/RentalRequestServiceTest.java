package com.quanlynhatro.rental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.rental.api.*;
import com.quanlynhatro.rental.domain.RentalRequestEntity;
import com.quanlynhatro.rental.repository.RentalRequestHistoryRepository;
import com.quanlynhatro.rental.repository.RentalRequestRepository;
import com.quanlynhatro.room.api.RoomStatus;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RentalRequestServiceTest {
    @Mock RentalRequestRepository requestRepository;
    @Mock RentalRequestHistoryRepository historyRepository;
    @Mock RoomRepository roomRepository;
    @Mock AppUserRepository userRepository;
    @Mock Authentication authentication;
    @Mock AppUserEntity tenant;
    @Mock AppUserEntity admin;
    @Mock RoleEntity tenantRole;
    @Mock RoleEntity adminRole;
    @Mock RoomEntity room;

    private RentalRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new RentalRequestServiceImpl(
                        requestRepository, historyRepository, roomRepository, userRepository);
        lenient().when(room.getId()).thenReturn(5L);
        lenient().when(room.getRoomCode()).thenReturn("P01");
        lenient().when(room.getStatus()).thenReturn(RoomStatus.VACANT.name());
        lenient().when(room.getMaxOccupants()).thenReturn(2);
    }

    @Test
    void tenantCanSubmitAndInitialHistoryIsWritten() {
        authenticateAs(tenant, tenantRole, "TENANT", "tenant");
        when(tenant.getId()).thenReturn(10L);
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(requestRepository.existsByApplicantUser_IdAndRoom_IdAndStatusIn(
                        eq(10L), eq(5L), anyCollection()))
                .thenReturn(false);
        when(requestRepository.existsByRequestCode(anyString())).thenReturn(false);
        when(requestRepository.save(any(RentalRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RentalRequestResponse response = service.submit(authentication, 5L, createRequest(1));

        assertEquals(RentalRequestStatus.PENDING, response.status());
        verify(historyRepository).save(any());
    }

    @Test
    void duplicateOpenRequestIsRejected() {
        authenticateAs(tenant, tenantRole, "TENANT", "tenant");
        when(tenant.getId()).thenReturn(10L);
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(requestRepository.existsByApplicantUser_IdAndRoom_IdAndStatusIn(
                        eq(10L), eq(5L), anyCollection()))
                .thenReturn(true);

        assertThrows(
                RentalRequestBusinessException.class,
                () -> service.submit(authentication, 5L, createRequest(1)));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void nonVacantRoomIsRejected() {
        authenticateAs(tenant, tenantRole, "TENANT", "tenant");
        when(room.getStatus()).thenReturn(RoomStatus.OCCUPIED.name());
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(room));

        assertThrows(
                RentalRequestBusinessException.class,
                () -> service.submit(authentication, 5L, createRequest(1)));
    }

    @Test
    void overCapacityIsRejected() {
        authenticateAs(tenant, tenantRole, "TENANT", "tenant");
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        assertThrows(
                RentalRequestBusinessException.class,
                () -> service.submit(authentication, 5L, createRequest(3)));
    }

    @Test
    void tenantCannotAccessAnotherTenantRequest() {
        authenticateAs(tenant, tenantRole, "TENANT", "tenant");
        when(tenant.getId()).thenReturn(10L);
        RentalRequestEntity request =
                RentalRequestEntity.create(
                        "RR-20261019-ABC12345",
                        otherUser(),
                        room,
                        "Other",
                        "0900000000",
                        null,
                        1,
                        LocalDate.now(),
                        null);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(
                RentalRequestAccessDeniedException.class,
                () -> service.getMyRequest(authentication, 1L));
    }

    @Test
    void tenantCanCancelOpenRequestAndHistoryIsWritten() {
        authenticateAs(tenant, tenantRole, "TENANT", "tenant");
        when(tenant.getId()).thenReturn(10L);
        RentalRequestEntity request =
                RentalRequestEntity.create(
                        "RR-20261019-ABC12345",
                        tenant,
                        room,
                        "Tenant",
                        "0900000000",
                        null,
                        1,
                        LocalDate.now(),
                        null);
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));

        RentalRequestResponse response = service.cancel(authentication, 1L);

        assertEquals(RentalRequestStatus.CANCELLED, response.status());
        verify(historyRepository).save(any());
        verify(requestRepository).findByIdForUpdate(1L);
    }

    @Test
    void tenantCanSupplementNeedsInfoRequestWithoutChangingStatusHistory() {
        authenticateAs(tenant, tenantRole, "TENANT", "tenant");
        when(tenant.getId()).thenReturn(10L);
        RentalRequestEntity request = mock(RentalRequestEntity.class);
        when(request.getStatus()).thenReturn(RentalRequestStatus.NEEDS_INFO);
        when(request.getApplicantUser()).thenReturn(tenant);
        when(request.getRoom()).thenReturn(room);
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(room));

        service.updateInformation(
                authentication,
                1L,
                new RentalRequestInfoUpdateRequest(
                        "Tenant updated",
                        "0911111111",
                        null,
                        1,
                        LocalDate.of(2026, 12, 1),
                        "Supplemented"));

        verify(request)
                .updateInformation(
                        "Tenant updated",
                        "0911111111",
                        null,
                        1,
                        LocalDate.of(2026, 12, 1),
                        "Supplemented");
        verify(historyRepository, never()).save(any());
    }

    @Test
    void adminCanRejectOrRequestMoreInformationAndHistoryIsWritten() {
        authenticateAs(admin, adminRole, "ADMIN", "admin");
        RentalRequestEntity request =
                RentalRequestEntity.create(
                        "RR-20261019-ABC12345",
                        tenant,
                        room,
                        "Tenant",
                        "0900000000",
                        null,
                        1,
                        LocalDate.now(),
                        null);
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));

        RentalRequestResponse response =
                service.decide(
                        authentication,
                        1L,
                        new RentalRequestDecisionRequest(
                                RentalRequestStatus.NEEDS_INFO, "Bổ sung CCCD"));

        assertEquals(RentalRequestStatus.NEEDS_INFO, response.status());
        verify(historyRepository).save(any());
    }

    @Test
    void requestCannotBeProcessedAfterItIsAlreadyFinalized() {
        authenticateAs(admin, adminRole, "ADMIN", "admin");
        RentalRequestEntity request =
                RentalRequestEntity.create(
                        "RR-20261019-ABC12345",
                        tenant,
                        room,
                        "Tenant",
                        "0900000000",
                        null,
                        1,
                        LocalDate.now(),
                        null);
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        service.decide(
                authentication,
                1L,
                new RentalRequestDecisionRequest(RentalRequestStatus.APPROVED, null));

        assertThrows(
                RentalRequestBusinessException.class,
                () ->
                        service.decide(
                                authentication,
                                1L,
                                new RentalRequestDecisionRequest(
                                        RentalRequestStatus.REJECTED, "Too late")));
    }

    @Test
    void requestCodeCollisionIsCheckedBeforeSaving() {
        authenticateAs(tenant, tenantRole, "TENANT", "tenant");
        when(tenant.getId()).thenReturn(10L);
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(requestRepository.existsByApplicantUser_IdAndRoom_IdAndStatusIn(
                        eq(10L), eq(5L), anyCollection()))
                .thenReturn(false);
        when(requestRepository.existsByRequestCode(anyString())).thenReturn(true, false);
        when(requestRepository.save(any(RentalRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.submit(authentication, 5L, createRequest(1));

        verify(requestRepository, times(2)).existsByRequestCode(anyString());
        verify(requestRepository).save(any(RentalRequestEntity.class));
    }

    @Test
    void adminSearchPassesFiltersToRepository() {
        authenticateAs(admin, adminRole, "ADMIN", "admin");
        when(requestRepository.search(
                        eq("P01"),
                        eq(RentalRequestStatus.PENDING),
                        eq(10L),
                        eq(5L),
                        any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        service.searchForAdmin(
                authentication,
                "  P01  ",
                RentalRequestStatus.PENDING,
                10L,
                5L,
                PageRequest.of(0, 10));

        verify(requestRepository)
                .search("P01", RentalRequestStatus.PENDING, 10L, 5L, PageRequest.of(0, 10));
    }

    @Test
    void adminCanApproveOnlyWhenRoomStillVacant() {
        authenticateAs(admin, adminRole, "ADMIN", "admin");
        when(admin.getId()).thenReturn(99L);
        RentalRequestEntity request =
                RentalRequestEntity.create(
                        "RR-20261019-ABC12345",
                        tenant,
                        room,
                        "Tenant",
                        "0900000000",
                        null,
                        1,
                        LocalDate.now(),
                        null);
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(room));

        RentalRequestResponse response =
                service.decide(
                        authentication,
                        1L,
                        new RentalRequestDecisionRequest(RentalRequestStatus.APPROVED, null));

        assertEquals(RentalRequestStatus.APPROVED, response.status());
        verify(room, never()).changeStatus(any());
        verify(historyRepository).save(any());
    }

    @Test
    void adminCannotApproveNonVacantRoom() {
        authenticateAs(admin, adminRole, "ADMIN", "admin");
        RentalRequestEntity request =
                RentalRequestEntity.create(
                        "RR-20261019-ABC12345",
                        tenant,
                        room,
                        "Tenant",
                        "0900000000",
                        null,
                        1,
                        LocalDate.now(),
                        null);
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(room.getStatus()).thenReturn(RoomStatus.OCCUPIED.name());

        assertThrows(
                RentalRequestBusinessException.class,
                () ->
                        service.decide(
                                authentication,
                                1L,
                                new RentalRequestDecisionRequest(
                                        RentalRequestStatus.APPROVED, null)));
        verify(historyRepository, never()).save(any());
    }

    @Test
    void tenantRoleCannotUseAdminOperation() {
        authenticateAs(tenant, tenantRole, "TENANT", "tenant");
        assertThrows(
                RentalRequestAccessDeniedException.class,
                () -> service.searchForAdmin(authentication, null, null, null, null, null));
    }

    private RentalRequestCreateRequest createRequest(int occupants) {
        return new RentalRequestCreateRequest(
                "Tenant",
                "0900000000",
                "tenant@example.com",
                occupants,
                LocalDate.of(2026, 11, 1),
                null);
    }

    private void authenticateAs(AppUserEntity user, RoleEntity role, String roleCode, String name) {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(name);
        when(user.isActive()).thenReturn(true);
        when(user.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn(roleCode);
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(name, name))
                .thenReturn(Optional.of(user));
    }

    private AppUserEntity otherUser() {
        AppUserEntity other = mock(AppUserEntity.class);
        when(other.getId()).thenReturn(11L);
        return other;
    }
}
