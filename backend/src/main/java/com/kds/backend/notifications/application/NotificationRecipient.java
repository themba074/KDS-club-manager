package com.kds.backend.notifications.application;

import java.util.UUID;
public record NotificationRecipient(UUID membershipId,String email,String displayName) {}
