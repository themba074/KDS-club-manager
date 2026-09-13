package com.kds.backend.notifications.application;

import com.kds.backend.identity.application.*;
import com.kds.backend.notifications.domain.*;
import com.kds.backend.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTests {
    private final NotificationRepository repository=mock(NotificationRepository.class);
    private final MembershipLifecycleService memberships=mock(MembershipLifecycleService.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-09-13T08:00:00Z"),ZoneOffset.UTC);
    private final NotificationService service=new NotificationService(repository,memberships,clock);
    private final UUID club=UUID.randomUUID(),actor=UUID.randomUUID(),member=UUID.randomUUID();
    @BeforeEach void setup(){TenantContext.set(club);when(memberships.requireCurrentMembership(actor)).thenReturn(new MembershipLifecycleMember(member,actor,"MEMBER","ACTIVE"));}
    @AfterEach void clear(){TenantContext.clear();}
    @Test void feedAndUnreadAlwaysUseAuthenticatedMembership(){service.feed(actor);service.unreadCount(actor);verify(repository).feed(member,clock.instant());verify(repository).unreadCount(member,clock.instant());}
    @Test void targetedReadCannotUseAnotherMembership(){when(repository.find(any(),eq(member),eq(clock.instant()))).thenReturn(Optional.empty());assertThrows(AccessDeniedException.class,()->service.markRead(actor,UUID.randomUUID()));}
    @Test void duplicateEventsCreateOnlyOneNotification(){when(repository.exists(anyString())).thenReturn(false,true);var recipient=new NotificationRecipient(member,"member@example.test","Member");service.create(NotificationType.MEETING_SCHEDULED,"Meeting","Body","/meetings","MEETING",UUID.randomUUID(),clock.instant(),List.of(recipient,recipient));verify(repository,times(1)).add(any());}
}
