package com.kds.backend.notifications.application;

import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.notifications.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;

@Service
public class NotificationEmailDispatcher {
    private final NotificationRepository notifications;private final NotificationEmailSender emails;private final Clock clock;private final String frontendUrl;
    public NotificationEmailDispatcher(NotificationRepository notifications,NotificationEmailSender emails,Clock clock,@Value("${app.auth.frontend-url}") String frontendUrl){this.notifications=notifications;this.emails=emails;this.clock=clock;this.frontendUrl=frontendUrl;}
    @Transactional public void dispatch(UUID clubId){TenantContext.set(clubId);try{Instant now=clock.instant();for(var item:notifications.dueEmails(now,3)){try{String link=item.getTargetPath()==null?"":"\n\nOpen: "+frontendUrl+item.getTargetPath();emails.send(item.getRecipientEmail(),item.getTitle(),item.getMessage()+link);item.markEmailDelivered(now);}catch(RuntimeException failure){item.markEmailFailed(now,failure.getMessage());}}}finally{TenantContext.clear();}}
}
