package com.kds.backend.members.infrastructure;

import com.kds.backend.members.application.MemberInvitationDelivery;
import com.kds.backend.notifications.application.NotificationEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component @Primary @ConditionalOnProperty(name="app.notifications.email-provider",havingValue="smtp")
public class SmtpMemberInvitationDelivery implements MemberInvitationDelivery {
    private final NotificationEmailSender emails;private final String frontendUrl;
    public SmtpMemberInvitationDelivery(NotificationEmailSender emails,@Value("${app.auth.frontend-url}") String frontendUrl){this.emails=emails;this.frontendUrl=frontendUrl;}
    @Override public void deliver(String email,String rawToken){emails.send(email,"You are invited to a KDS club","Accept your invitation: "+frontendUrl+"/accept-invitation?token="+rawToken);}
}
