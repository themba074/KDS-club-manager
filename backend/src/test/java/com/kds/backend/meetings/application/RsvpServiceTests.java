package com.kds.backend.meetings.application;

import com.kds.backend.identity.application.*;
import com.kds.backend.meetings.domain.*;
import com.kds.backend.meetings.repository.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RsvpServiceTests {
    private final MeetingRepository meetings=mock(MeetingRepository.class);private final MeetingParticipationRepository participation=mock(MeetingParticipationRepository.class);private final ClubService clubs=mock(ClubService.class);private final MembershipLifecycleService memberships=mock(MembershipLifecycleService.class);private final Clock clock=Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"),ZoneOffset.UTC);private final RsvpService service=new RsvpService(meetings,participation,clubs,memberships,clock);private final UUID club=UUID.randomUUID(),actor=UUID.randomUUID(),meetingId=UUID.randomUUID(),membershipId=UUID.randomUUID();
    @BeforeEach void setup(){TenantContext.set(club);when(clubs.requireMembership(actor,club)).thenReturn(new ClubSummary(club,"Club","INVESTMENT_CLUB",false,List.of("MEETINGS_READ")));when(memberships.requireCurrentMembership(actor)).thenReturn(new MembershipLifecycleMember(membershipId,actor,"MEMBER","ACTIVE"));var meeting=new MeetingEntity(meetingId,club,actor,clock.instant());meeting.update("Meeting",null,clock.instant().plusSeconds(3600),0,60,"Hall",null,List.of(new MeetingEntity.AgendaDraft("Item",null)),clock.instant());when(meetings.find(meetingId)).thenReturn(Optional.of(meeting));}
    @AfterEach void clear(){TenantContext.clear();}
    @Test void responseIsAlwaysBoundToAuthenticatedMembership(){service.respond(actor,meetingId,MeetingRsvpEntity.Response.YES);verify(participation).add(argThat((MeetingRsvpEntity value)->value.getMembershipId().equals(membershipId)&&value.getResponse()==MeetingRsvpEntity.Response.YES));}
    @Test void existingResponseIsUpdatedInsteadOfDuplicated(){var existing=mock(MeetingRsvpEntity.class);when(participation.rsvp(meetingId,membershipId)).thenReturn(Optional.of(existing));service.respond(actor,meetingId,MeetingRsvpEntity.Response.NO);verify(existing).change(MeetingRsvpEntity.Response.NO,clock.instant());verify(participation,never()).add(any(MeetingRsvpEntity.class));}
}
