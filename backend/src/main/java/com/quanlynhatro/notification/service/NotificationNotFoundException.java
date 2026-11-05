package com.quanlynhatro.notification.service;

public class NotificationNotFoundException extends NotificationBusinessException {
    public NotificationNotFoundException() {
        super("Notification not found");
    }
}
