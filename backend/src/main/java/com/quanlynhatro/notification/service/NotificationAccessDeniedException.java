package com.quanlynhatro.notification.service;

public class NotificationAccessDeniedException extends NotificationBusinessException {
    public NotificationAccessDeniedException() {
        super("Notification access denied");
    }
}
