package com.kds.backend.meetings.infrastructure;
import com.kds.backend.meetings.application.MeetingChanged;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
public class DevelopmentMeetingNotificationListener {
    private static final Logger LOGGER=LoggerFactory.getLogger(DevelopmentMeetingNotificationListener.class);
    @TransactionalEventListener
    public void notifyAfterCommit(MeetingChanged change){LOGGER.info("meeting_notification_stub type={} clubId={} meetingId={} recipients={} startsAt={}",change.type(),change.clubId(),change.meetingId(),change.recipients().size(),change.startsAt());}
}
