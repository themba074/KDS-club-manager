package com.kds.backend.notifications.application;

import com.kds.backend.contributions.application.ContributionReminderRequested;
import com.kds.backend.meetings.application.*;
import com.kds.backend.members.application.*;
import com.kds.backend.notifications.domain.NotificationType;
import com.kds.backend.voting.application.MotionNotificationChanged;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationEventHandlerTests {
    private final NotificationService notifications=mock(NotificationService.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-09-13T08:00:00Z"),ZoneOffset.UTC);
    private final NotificationEventHandler handler=new NotificationEventHandler(notifications,clock);
    private final UUID club=UUID.randomUUID(),source=UUID.randomUUID(),member=UUID.randomUUID();
    @AfterEach void clear(){com.kds.backend.identity.application.TenantContext.clear();}
    @Test void mapsMeetingAndMinutesTriggers(){
        var audience=List.of(new MeetingAudienceMember(member,"member@example.test","Member"));
        handler.meeting(new MeetingChanged(MeetingChanged.Type.CREATED,club,source,"Monthly meeting",OffsetDateTime.now(clock),audience));
        verify(notifications).create(eq(NotificationType.MEETING_SCHEDULED),eq("Meeting scheduled"),contains("Monthly meeting"),eq("/meetings"),eq("MEETING"),eq(source),eq(clock.instant()),anyList());
        handler.minutes(new MinutesPublished(club,source,"Monthly meeting",audience));
        verify(notifications).create(eq(NotificationType.MINUTES_PUBLISHED),eq("Minutes published"),contains("Monthly meeting"),eq("/meetings"),eq("MINUTES"),eq(source),eq(clock.instant()),anyList());
    }
    @Test void mapsContributionReminderAndKeepsFailuresBestEffort(){
        doThrow(new IllegalStateException("database offline")).when(notifications).create(any(),any(),any(),any(),any(),any(),any(),any());
        assertDoesNotThrow(()->handler.contribution(new ContributionReminderRequested(club,source,member,"member@example.test","Member","Monthly",LocalDate.parse("2026-09-01"),new BigDecimal("75.00"),"ZAR")));
        verify(notifications).create(eq(NotificationType.PAYMENT_REMINDER),any(),contains("75.00"),eq("/contributions"),eq("CONTRIBUTION"),eq(source),eq(clock.instant()),anyList());
    }
    @Test void schedulesVoteOpeningAndClosingAndCancelsPendingWork(){
        Instant opens=clock.instant().plusSeconds(3600),closes=opens.plusSeconds(4*3600);
        var voters=List.of(new VotingEligibleMember(member,"Member","member@example.test"));
        handler.motion(new MotionNotificationChanged(MotionNotificationChanged.Action.SCHEDULED,club,source,"Budget vote",opens,closes,voters));
        @SuppressWarnings("unchecked") var plans=(List<NotificationService.ScheduledNotification>)mockingDetails(notifications).getInvocations().stream().filter(call->call.getMethod().getName().equals("replaceScheduled")).findFirst().orElseThrow().getArgument(2);
        assertEquals(NotificationType.VOTE_OPEN,plans.get(0).type());assertEquals(opens,plans.get(0).availableAt());
        assertEquals(NotificationType.VOTE_CLOSING,plans.get(1).type());assertEquals(closes.minus(Duration.ofHours(1)),plans.get(1).availableAt());
        handler.motion(new MotionNotificationChanged(MotionNotificationChanged.Action.CANCELLED,club,source,"Budget vote",opens,closes,voters));
        verify(notifications).cancelScheduled("MOTION",source);
    }
}
