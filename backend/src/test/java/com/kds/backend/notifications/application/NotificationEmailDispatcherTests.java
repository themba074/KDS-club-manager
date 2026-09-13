package com.kds.backend.notifications.application;

import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.notifications.domain.*;
import com.kds.backend.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationEmailDispatcherTests {
    @Test void oneEmailFailureDoesNotPreventOtherDeliveries(){
        var repository=mock(NotificationRepository.class);var sender=mock(NotificationEmailSender.class);
        var now=Instant.parse("2026-09-13T08:00:00Z");var club=UUID.randomUUID();
        var first=item(club,"first@example.test",now);var second=item(club,"second@example.test",now);
        when(repository.dueEmails(now,3)).thenReturn(List.of(first,second));doThrow(new IllegalStateException("SMTP offline")).when(sender).send(eq("first@example.test"),any(),any());
        assertDoesNotThrow(()->new NotificationEmailDispatcher(repository,sender,Clock.fixed(now,ZoneOffset.UTC),"http://localhost:5175").dispatch(club));
        assertEquals(1,first.getEmailAttempts());assertNull(first.getEmailedAt());assertEquals("SMTP offline",first.getEmailError());
        assertEquals(now,second.getEmailedAt());assertNull(TenantContext.currentClubId());
    }
    private NotificationEntity item(UUID club,String email,Instant now){return new NotificationEntity(UUID.randomUUID(),club,UUID.randomUUID(),email,NotificationType.MEETING_SCHEDULED,"Meeting","Body","/meetings","MEETING",UUID.randomUUID(),UUID.randomUUID().toString(),now,now);}
}
