package com.kds.backend.members.infrastructure;

import com.kds.backend.members.application.MemberInvitationDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.members.development-invitation-delivery", havingValue = "true")
public class DevelopmentMemberInvitationDelivery implements MemberInvitationDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevelopmentMemberInvitationDelivery.class);
    private final String frontendUrl;
    public DevelopmentMemberInvitationDelivery(@Value("${app.auth.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }
    @Override public void deliver(String email, String rawToken) {
        LOGGER.warn("DEVELOPMENT ONLY member invitation for {}: {}/accept-invitation?token={}", email, frontendUrl, rawToken);
    }
}
