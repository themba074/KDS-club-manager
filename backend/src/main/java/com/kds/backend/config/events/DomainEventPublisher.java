package com.kds.backend.config.events;

/** Shared application boundary used by modules to publish best-effort internal events. */
public interface DomainEventPublisher {
    void publish(Object event);
}
