package com.kds.backend.notifications.application;

public interface NotificationEmailSender {
    void send(String recipient,String subject,String body);
}
