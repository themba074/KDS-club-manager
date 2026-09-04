package com.kds.backend.meetings.application;

import com.kds.backend.documents.application.*;
import com.kds.backend.identity.application.*;
import com.kds.backend.meetings.domain.*;
import com.kds.backend.meetings.repository.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MinutesServiceTests {
    private final MeetingRepository meetings=mock(MeetingRepository.class);private final MeetingParticipationRepository participation=mock(MeetingParticipationRepository.class);private final ClubService clubs=mock(ClubService.class);private final FileStorageService storage=mock(FileStorageService.class);private final Clock clock=Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"),ZoneOffset.UTC);private final MinutesService service=new MinutesService(meetings,participation,clubs,storage,clock);private final UUID club=UUID.randomUUID(),actor=UUID.randomUUID(),meetingId=UUID.randomUUID();
    @BeforeEach void setup(){TenantContext.set(club);when(clubs.requireMembership(actor,club)).thenReturn(new ClubSummary(club,"Club","INVESTMENT_CLUB",true,List.of("MEETINGS_READ","MEETINGS_WRITE")));var meeting=new MeetingEntity(meetingId,club,actor,clock.instant().minusSeconds(7200));meeting.update("Meeting",null,clock.instant().minusSeconds(3600),0,60,"Hall",null,List.of(new MeetingEntity.AgendaDraft("Item",null)),clock.instant());when(meetings.find(meetingId)).thenReturn(Optional.of(meeting));}
    @AfterEach void clear(){TenantContext.clear();}
    @Test void unsafeOrOversizedAttachmentsAreRejectedBeforeStorage(){var exception=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.attach(actor,meetingId,0,new MinutesAttachment("bad.exe","application/octet-stream",new byte[]{1})));assertEquals(400,exception.getStatusCode().value());verifyNoInteractions(storage);}
    @Test void staleDraftCannotOverwriteCurrentMinutes(){var minutes=mock(MeetingMinutesEntity.class);when(minutes.getVersion()).thenReturn(2L);when(participation.minutes(meetingId)).thenReturn(Optional.of(minutes));var exception=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.save(actor,meetingId,1,"New text"));assertEquals(409,exception.getStatusCode().value());verify(minutes,never()).updateBody(any(),any());}
}
