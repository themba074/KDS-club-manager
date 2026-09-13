package com.kds.backend.notifications.infrastructure;

import com.kds.backend.notifications.application.NotificationEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component @ConditionalOnProperty(name="app.notifications.email-provider",havingValue="smtp")
public class SmtpNotificationEmailSender implements NotificationEmailSender {
    private final JavaMailSender sender; private final String from;
    public SmtpNotificationEmailSender(JavaMailSender sender,@Value("${app.notifications.from-address}") String from){this.sender=sender;this.from=from;}
    @Override public void send(String recipient,String subject,String body){var message=new SimpleMailMessage();message.setFrom(from);message.setTo(recipient);message.setSubject(subject);message.setText(body);sender.send(message);}
}
