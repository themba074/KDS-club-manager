package com.kds.backend.identity.infrastructure;

import com.kds.backend.identity.application.PasswordResetDelivery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.auth.development-reset-delivery", havingValue = "false", matchIfMissing = true)
public class DisabledPasswordResetDelivery implements PasswordResetDelivery {
    @Override public void deliver(String email, String rawToken) { }
}
