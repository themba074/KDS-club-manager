package com.kds.backend.identity.infrastructure;

import com.kds.backend.identity.application.PasswordResetDelivery;
import com.kds.backend.notifications.application.NotificationEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component @Primary @ConditionalOnProperty(name="app.notifications.email-provider",havingValue="smtp")
public class SmtpPasswordResetDelivery implements PasswordResetDelivery {
    private final NotificationEmailSender emails;private final String frontendUrl;
    public SmtpPasswordResetDelivery(NotificationEmailSender emails,@Value("${app.auth.frontend-url}") String frontendUrl){this.emails=emails;this.frontendUrl=frontendUrl;}
    @Override public void deliver(String email,String rawToken){emails.send(email,"Reset your KDS Club Manager password","Reset your password: "+frontendUrl+"/reset-password?token="+rawToken);}
}
