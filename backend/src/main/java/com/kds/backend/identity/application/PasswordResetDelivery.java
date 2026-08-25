package com.kds.backend.identity.application;

public interface PasswordResetDelivery {
    void deliver(String email, String rawToken);
}
