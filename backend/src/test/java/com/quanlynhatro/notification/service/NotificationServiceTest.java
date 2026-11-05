package com.quanlynhatro.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.notification.api.NotificationCreateRequest;
import com.quanlynhatro.notification.domain.NotificationEntity;
import com.quanlynhatro.notification.repository.NotificationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {
    @Mock NotificationRepository notifications;
    @Mock AppUserRepository users;
    @InjectMocks NotificationServiceImpl service;

    @Test
    void createForTenantAndRelatedNotificationIsSaved() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(users.findByIdAndRole_Code(2L, "TENANT")).thenReturn(Optional.of(tenant));
        when(notifications
                        .existsByRecipientUser_IdAndNotificationTypeAndRelatedEntityTypeAndRelatedEntityId(
                                2L, "INVOICE", "INVOICE", 9L))
                .thenReturn(false);
        when(notifications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                service.create(
                        new NotificationCreateRequest(
                                2L,
                                "INVOICE",
                                "Hóa đơn mới",
                                "Hóa đơn đã phát hành",
                                "INVOICE",
                                9L),
                        1L);

        assertEquals("INVOICE", response.notificationType());
        assertEquals(2L, response.recipientUserId());
        verify(notifications).save(any(NotificationEntity.class));
    }

    @Test
    void repeatedRelatedNotificationIsRejectedWithoutSaving() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(users.findByIdAndRole_Code(2L, "TENANT")).thenReturn(Optional.of(tenant));
        when(notifications
                        .existsByRecipientUser_IdAndNotificationTypeAndRelatedEntityTypeAndRelatedEntityId(
                                2L, "PAYMENT", "PAYMENT", 8L))
                .thenReturn(true);

        assertThrows(
                NotificationConflictException.class,
                () ->
                        service.create(
                                new NotificationCreateRequest(
                                        2L, "PAYMENT", "Thanh toán", "Đã xác nhận", "PAYMENT", 8L),
                                1L));
        verify(notifications, never()).save(any());
    }

    @Test
    void tenantCanMarkOnlyOwnNotificationRead() {
        AppUserEntity tenant = user(2L, "TENANT");
        NotificationEntity notification =
                NotificationEntity.create(tenant, "INVOICE", "Title", "Content", "INVOICE", 3L);
        when(users.findByIdAndRole_Code(2L, "TENANT")).thenReturn(Optional.of(tenant));
        when(notifications.findByIdAndRecipientUser_Id(7L, 2L))
                .thenReturn(Optional.of(notification));

        var response = service.markRead(7L, 2L);

        assertTrue(response.read());
        assertNotNull(response.readAt());
    }

    @Test
    void notificationOfAnotherTenantIsNotFound() {
        AppUserEntity tenant = user(2L, "TENANT");
        when(users.findByIdAndRole_Code(2L, "TENANT")).thenReturn(Optional.of(tenant));
        when(notifications.findByIdAndRecipientUser_Id(7L, 2L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> service.getForTenant(7L, 2L));
    }

    @Test
    void tenantSearchUsesRecipientAndUnreadFilter() {
        AppUserEntity tenant = user(2L, "TENANT");
        when(users.findByIdAndRole_Code(2L, "TENANT")).thenReturn(Optional.of(tenant));
        when(notifications.searchForRecipient(eq(2L), eq(true), eq("invoice"), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        service.searchForTenant(
                new com.quanlynhatro.notification.api.NotificationQuery("invoice", true),
                2L,
                PageRequest.of(0, 10));

        verify(notifications).searchForRecipient(eq(2L), eq(true), eq("invoice"), any());
    }

    @Test
    void notifyAdminsCreatesOneUnreadNotificationPerActiveAdmin() {
        AppUserEntity admin = user(1L, "ADMIN");
        when(users.findAllByRole_CodeAndActiveTrue("ADMIN")).thenReturn(List.of(admin));
        when(notifications
                        .existsByRecipientUser_IdAndNotificationTypeAndRelatedEntityTypeAndRelatedEntityId(
                                1L, "MAINTENANCE_REQUEST", "MAINTENANCE_REQUEST", 12L))
                .thenReturn(false);
        when(notifications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.notifyAdmins(
                "MAINTENANCE_REQUEST",
                "Yêu cầu bảo trì",
                "Có yêu cầu mới",
                "MAINTENANCE_REQUEST",
                12L);

        verify(notifications).save(any(NotificationEntity.class));
    }

    private AppUserEntity user(Long id, String code) {
        AppUserEntity user = mock(AppUserEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn(code);
        return user;
    }
}
