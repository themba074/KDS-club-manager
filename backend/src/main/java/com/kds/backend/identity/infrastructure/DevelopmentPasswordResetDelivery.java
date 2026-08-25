package com.kds.backend.identity.infrastructure;

import com.kds.backend.identity.application.PasswordResetDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.auth.development-reset-delivery", havingValue = "true")
public class DevelopmentPasswordResetDelivery implements PasswordResetDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevelopmentPasswordResetDelivery.class);
    private final String frontendUrl;
    public DevelopmentPasswordResetDelivery(@Value("${app.auth.frontend-url}") String frontendUrl) { this.frontendUrl = frontendUrl; }
    @Override public void deliver(String email, String rawToken) {
        LOGGER.warn("DEVELOPMENT ONLY password reset for {}: {}/reset-password?token={}", email, frontendUrl, rawToken);
    }
}
