package com.kds.backend.meetings.infrastructure;
import com.kds.backend.meetings.application.*;
import com.kds.backend.config.events.DomainEventPublisher;
import org.springframework.stereotype.Component;
@Component
public class SpringMeetingNotificationPublisher implements MeetingNotificationPublisher {
    private final DomainEventPublisher events;
    public SpringMeetingNotificationPublisher(DomainEventPublisher events){this.events=events;}
    public void publish(MeetingChanged change){events.publish(change);}
}
