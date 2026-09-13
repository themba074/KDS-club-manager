package com.kds.backend.notifications.application;

import com.kds.backend.notifications.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationView(UUID id,NotificationType type,String title,String message,String targetPath,
                               Instant availableAt,Instant readAt) {}
