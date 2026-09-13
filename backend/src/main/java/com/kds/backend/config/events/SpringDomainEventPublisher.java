package com.kds.backend.config.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {
    private final ApplicationEventPublisher events;
    public SpringDomainEventPublisher(ApplicationEventPublisher events){this.events=events;}
    @Override public void publish(Object event){events.publishEvent(event);}
}
