package com.kds.backend.members.infrastructure;

import com.kds.backend.members.application.MemberInvitationDelivery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.members.development-invitation-delivery", havingValue = "false", matchIfMissing = true)
public class DisabledMemberInvitationDelivery implements MemberInvitationDelivery {
    @Override public void deliver(String email, String rawToken) { }
}
