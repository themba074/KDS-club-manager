package com.kds.backend.notifications.infrastructure;

import com.kds.backend.notifications.application.NotificationEmailSender;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component @ConditionalOnProperty(name="app.notifications.email-provider",havingValue="log",matchIfMissing=true)
public class LoggingNotificationEmailSender implements NotificationEmailSender {
    private static final Logger LOGGER=LoggerFactory.getLogger(LoggingNotificationEmailSender.class);
    @Override public void send(String recipient,String subject,String body){LOGGER.info("notification_email recipient={} subject={} body={}",recipient,subject,body);}
}
