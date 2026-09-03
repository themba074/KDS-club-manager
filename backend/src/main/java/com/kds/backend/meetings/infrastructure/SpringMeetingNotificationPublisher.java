package com.kds.backend.meetings.infrastructure;
import com.kds.backend.meetings.application.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
@Component
public class SpringMeetingNotificationPublisher implements MeetingNotificationPublisher {
    private final ApplicationEventPublisher events;
    public SpringMeetingNotificationPublisher(ApplicationEventPublisher events){this.events=events;}
    public void publish(MeetingChanged change){events.publishEvent(change);}
}
