package com.kds.backend.notifications.application;

import com.kds.backend.contributions.application.ContributionReminderRequested;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.meetings.application.*;
import com.kds.backend.notifications.domain.NotificationType;
import com.kds.backend.voting.application.MotionNotificationChanged;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import java.time.*;
import java.util.List;

@Component
public class NotificationEventHandler {
    private static final Logger LOGGER=LoggerFactory.getLogger(NotificationEventHandler.class);
    private final NotificationService notifications;private final Clock clock;
    public NotificationEventHandler(NotificationService notifications,Clock clock){this.notifications=notifications;this.clock=clock;}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void meeting(MeetingChanged event){safely(event.clubId(),()->notifications.create(event.type()==MeetingChanged.Type.CREATED?NotificationType.MEETING_SCHEDULED:NotificationType.MEETING_UPDATED,event.type()==MeetingChanged.Type.CREATED?"Meeting scheduled":"Meeting updated",event.title()+" · "+event.startsAt(),"/meetings","MEETING",event.meetingId(),clock.instant(),event.recipients().stream().map(r->new NotificationRecipient(r.membershipId(),r.email(),r.displayName())).toList()));}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void minutes(MinutesPublished event){safely(event.clubId(),()->notifications.create(NotificationType.MINUTES_PUBLISHED,"Minutes published","Minutes for "+event.meetingTitle()+" are now available.","/meetings","MINUTES",event.meetingId(),clock.instant(),event.recipients().stream().map(r->new NotificationRecipient(r.membershipId(),r.email(),r.displayName())).toList()));}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void contribution(ContributionReminderRequested event){safely(event.clubId(),()->notifications.create(NotificationType.PAYMENT_REMINDER,"Contribution reminder",event.scheduleName()+" has "+event.currency()+" "+event.outstanding().toPlainString()+" outstanding, due "+event.dueDate()+".","/contributions","CONTRIBUTION",event.scheduleVersionId(),clock.instant(),List.of(new NotificationRecipient(event.membershipId(),event.recipientEmail(),event.memberName()))));}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void motion(MotionNotificationChanged event){safely(event.clubId(),()->{if(event.action()==MotionNotificationChanged.Action.CANCELLED){notifications.cancelScheduled("MOTION",event.motionId());return;}var recipients=event.recipients().stream().map(r->new NotificationRecipient(r.membershipId(),r.email(),r.displayName())).toList();Instant closing=event.closesAt().minus(Duration.ofHours(1));if(!closing.isAfter(event.opensAt()))closing=event.opensAt().plus(Duration.between(event.opensAt(),event.closesAt()).dividedBy(2));notifications.replaceScheduled("MOTION",event.motionId(),List.of(new NotificationService.ScheduledNotification(NotificationType.VOTE_OPEN,"Voting is open",event.title()+" is ready for your vote.","/voting",event.opensAt(),recipients),new NotificationService.ScheduledNotification(NotificationType.VOTE_CLOSING,"Voting closes soon",event.title()+" closes at "+event.closesAt()+".","/voting",closing,recipients)));});}
    private void safely(java.util.UUID club,Runnable action){var previous=TenantContext.currentClubId();TenantContext.set(club);try{action.run();}catch(RuntimeException failure){LOGGER.warn("notification_event_failed clubId={}",club,failure);}finally{if(previous==null)TenantContext.clear();else TenantContext.set(previous);}}
}
