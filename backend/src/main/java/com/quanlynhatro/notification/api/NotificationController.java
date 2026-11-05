package com.quanlynhatro.notification.api;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.notification.service.NotificationAccessDeniedException;
import com.quanlynhatro.notification.service.NotificationService;
import com.quanlynhatro.room.api.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class NotificationController {
    private final NotificationService notifications;
    private final AppUserRepository users;

    public NotificationController(NotificationService notifications, AppUserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @PostMapping("/admin/notifications")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(
            Authentication authentication, @Valid @RequestBody NotificationCreateRequest request) {
        return notifications.create(request, current(authentication, "ADMIN").getId());
    }

    @GetMapping("/admin/notifications")
    public PageResponse<NotificationResponse> adminList(
            Authentication authentication, Pageable pageable) {
        return PageResponse.from(
                notifications.searchForAdmin(pageable, current(authentication, "ADMIN").getId()));
    }

    @GetMapping("/admin/notifications/unread-count")
    public long adminUnreadCount(Authentication authentication) {
        return notifications.adminUnreadCount(current(authentication, "ADMIN").getId());
    }

    @PatchMapping("/admin/notifications/{id}/read")
    public NotificationResponse adminMarkRead(
            Authentication authentication, @PathVariable @Positive Long id) {
        return notifications.markReadForAdmin(id, current(authentication, "ADMIN").getId());
    }

    @GetMapping("/tenant/notifications")
    public PageResponse<NotificationResponse> tenantList(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean unread,
            Pageable pageable) {
        AppUserEntity tenant = current(authentication, "TENANT");
        return PageResponse.from(
                notifications.searchForTenant(
                        new NotificationQuery(keyword, unread), tenant.getId(), pageable));
    }

    @GetMapping("/tenant/notifications/unread-count")
    public long unreadCount(Authentication authentication) {
        return notifications.unreadCount(current(authentication, "TENANT").getId());
    }

    @GetMapping("/tenant/notifications/{id}")
    public NotificationResponse detail(
            Authentication authentication, @PathVariable @Positive Long id) {
        return notifications.getForTenant(id, current(authentication, "TENANT").getId());
    }

    @PatchMapping("/tenant/notifications/{id}/read")
    public NotificationResponse markRead(
            Authentication authentication, @PathVariable @Positive Long id) {
        return notifications.markRead(id, current(authentication, "TENANT").getId());
    }

    private AppUserEntity current(Authentication authentication, String role) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new NotificationAccessDeniedException();
        }
        AppUserEntity user =
                users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                authentication.getName(), authentication.getName())
                        .orElseThrow(NotificationAccessDeniedException::new);
        if (user.getRole() == null || !role.equals(user.getRole().getCode())) {
            throw new NotificationAccessDeniedException();
        }
        return user;
    }
}
