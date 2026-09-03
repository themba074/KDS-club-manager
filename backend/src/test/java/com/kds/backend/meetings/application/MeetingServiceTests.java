package com.kds.backend.meetings.application;
import com.kds.backend.identity.application.*;
import com.kds.backend.meetings.domain.MeetingEntity;
import com.kds.backend.meetings.repository.MeetingRepository;
import com.kds.backend.members.application.MemberService;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MeetingServiceTests {
    private final MeetingRepository repository=mock(MeetingRepository.class);private final ClubService clubs=mock(ClubService.class);
    private final MemberService members=mock(MemberService.class);private final MeetingMapper mapper=mock(MeetingMapper.class);
    private final MeetingNotificationPublisher notifications=mock(MeetingNotificationPublisher.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-09-03T08:00:00Z"),ZoneOffset.UTC);
    private final MeetingService service=new MeetingService(repository,clubs,members,mapper,notifications,clock);
    private final UUID actor=UUID.randomUUID(),clubId=UUID.randomUUID();
    @BeforeEach void context(){TenantContext.set(clubId);}
    @AfterEach void clear(){TenantContext.clear();}
    @Test void agendaOrderIsServerAssignedAndReplacementKeepsStablePositions(){
        var meeting=new MeetingEntity(UUID.randomUUID(),clubId,actor,clock.instant());
        meeting.update("Title",null,clock.instant().plusSeconds(3600),120,60,"Hall",null,List.of(new MeetingEntity.AgendaDraft("First",null),new MeetingEntity.AgendaDraft("Second",null)),clock.instant());
        UUID firstId=meeting.getAgendaItems().getFirst().getId();
        meeting.update("Title",null,clock.instant().plusSeconds(7200),120,60,"Hall",null,List.of(new MeetingEntity.AgendaDraft("Moved",null)),clock.instant());
        assertEquals(1,meeting.getAgendaItems().size());assertEquals(0,meeting.getAgendaItems().getFirst().getPosition());assertEquals("Moved",meeting.getAgendaItems().getFirst().getTitle());assertEquals(firstId,meeting.getAgendaItems().getFirst().getId());
    }
    @Test void notificationFailureNeverRollsBackAValidCreate(){
        when(clubs.requireMembership(actor,clubId)).thenReturn(club("MEETINGS_WRITE"));doThrow(new IllegalStateException("offline")).when(notifications).publish(any());
        assertDoesNotThrow(()->service.create(actor,command(0)));verify(repository).add(any());verify(repository).flush();
    }
    @Test void currentPermissionsAreRecheckedAndPastMeetingsCannotBeEdited(){
        when(clubs.requireMembership(actor,clubId)).thenReturn(club());assertThrows(AccessDeniedException.class,()->service.create(actor,command(0)));
        when(clubs.requireMembership(actor,clubId)).thenReturn(club("MEETINGS_WRITE"));var past=new MeetingEntity(UUID.randomUUID(),clubId,actor,clock.instant().minusSeconds(7200));past.update("Past",null,clock.instant().minusSeconds(3600),120,60,"Hall",null,List.of(new MeetingEntity.AgendaDraft("Item",null)),clock.instant());when(repository.find(past.getId())).thenReturn(Optional.of(past));
        assertEquals(409,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.edit(actor,past.getId(),command(0))).getStatusCode().value());
    }
    @Test void staleVersionIsRejectedBeforeMutationOrNotification(){
        when(clubs.requireMembership(actor,clubId)).thenReturn(club("MEETINGS_WRITE"));var meeting=new MeetingEntity(UUID.randomUUID(),clubId,actor,clock.instant());meeting.update("Future",null,clock.instant().plusSeconds(7200),120,60,"Hall",null,List.of(new MeetingEntity.AgendaDraft("Item",null)),clock.instant());when(repository.find(meeting.getId())).thenReturn(Optional.of(meeting));
        assertEquals(409,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.edit(actor,meeting.getId(),command(1))).getStatusCode().value());verify(notifications,never()).publish(any());
    }
    private MeetingCommand command(long version){return new MeetingCommand(version,"Monthly meeting",null,OffsetDateTime.now(clock).plusDays(1),60,"Community hall",null,List.of(new MeetingCommand.AgendaItem("Welcome",null)));}
    private ClubSummary club(String...permissions){return new ClubSummary(clubId,"Club","INVESTMENT_CLUB",false,List.of(permissions));}
}
