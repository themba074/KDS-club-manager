package com.kds.backend.notifications.api;

import com.kds.backend.notifications.application.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/notifications") @PreAuthorize("isAuthenticated()")
public class NotificationController {
    private final NotificationService notifications;
    public NotificationController(NotificationService notifications){this.notifications=notifications;}
    @GetMapping public List<NotificationView> feed(@AuthenticationPrincipal Jwt jwt){return notifications.feed(actor(jwt));}
    @GetMapping("/unread-count") public UnreadCount unread(@AuthenticationPrincipal Jwt jwt){return new UnreadCount(notifications.unreadCount(actor(jwt)));}
    @PutMapping("/{id}/read") public NotificationView read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return notifications.markRead(actor(jwt),id);}
    @PutMapping("/read-all") public UpdatedCount readAll(@AuthenticationPrincipal Jwt jwt){return new UpdatedCount(notifications.markAllRead(actor(jwt)));}
    public record UnreadCount(long count){} public record UpdatedCount(int updated){}
    private static UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
