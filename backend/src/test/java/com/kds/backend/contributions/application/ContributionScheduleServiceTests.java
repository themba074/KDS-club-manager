package com.kds.backend.contributions.application;

import com.kds.backend.contributions.domain.ContributionScheduleVersionEntity;
import com.kds.backend.contributions.domain.ContributionScheduleVersionEntity.*;
import com.kds.backend.contributions.repository.ContributionScheduleRepository;
import com.kds.backend.identity.application.*;
import com.kds.backend.members.application.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContributionScheduleServiceTests {
    private final ContributionScheduleRepository repository=mock(ContributionScheduleRepository.class);
    private final MemberService members=mock(MemberService.class);
    private final ClubService clubs=mock(ClubService.class);
    private final ContributionScheduleMapper mapper=mock(ContributionScheduleMapper.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-08-29T08:00:00Z"),ZoneOffset.UTC);
    private final ContributionScheduleService service=new ContributionScheduleService(repository,members,clubs,mapper,clock);
    private final UUID clubId=UUID.randomUUID(),actor=UUID.randomUUID(),memberId=UUID.randomUUID();
    @BeforeEach void context(){TenantContext.set(clubId);}
    @AfterEach void clear(){TenantContext.clear();}

    @Test void monthlyCalculationUsesOriginalDayAndMonthEndSafely() {
        var version=version(Frequency.MONTHLY,LocalDate.of(2026,1,31),null,LocalDate.of(2026,1,1),null);
        assertEquals(List.of(LocalDate.of(2026,1,31),LocalDate.of(2026,2,28),LocalDate.of(2026,3,31)),
            ContributionScheduleService.dueDates(version,LocalDate.of(2026,1,1),LocalDate.of(2026,3,31)));
    }
    @Test void revisionBoundariesPreventDuplicateExpectations() {
        var old=version(Frequency.MONTHLY,LocalDate.of(2026,1,15),null,LocalDate.of(2026,1,1),LocalDate.of(2026,3,31));
        var replacement=version(Frequency.MONTHLY,LocalDate.of(2026,1,15),null,LocalDate.of(2026,4,1),null);
        assertEquals(List.of(LocalDate.of(2026,3,15)),ContributionScheduleService.dueDates(old,LocalDate.of(2026,3,1),LocalDate.of(2026,4,30)));
        assertEquals(List.of(LocalDate.of(2026,4,15)),ContributionScheduleService.dueDates(replacement,LocalDate.of(2026,3,1),LocalDate.of(2026,4,30)));
    }
    @Test void onceOffCalculationProducesExactlyOneDueDate() {
        var version=version(Frequency.ONCE_OFF,LocalDate.of(2026,9,10),null,LocalDate.of(2026,8,29),null);
        assertEquals(List.of(LocalDate.of(2026,9,10)),ContributionScheduleService.dueDates(version,LocalDate.of(2026,9,1),LocalDate.of(2026,9,30)));
        assertTrue(ContributionScheduleService.dueDates(version,LocalDate.of(2026,10,1),LocalDate.of(2026,10,31)).isEmpty());
    }
    @Test void createSnapshotsOnlyActiveMembersAndRechecksPermission() {
        when(clubs.requireMembership(actor,clubId)).thenReturn(club("CONTRIBUTIONS_WRITE"));
        when(members.lockAndSnapshotActiveContributionMembers()).thenReturn(List.of(new ContributionMember(memberId,"m@example.test","Member")));
        when(members.contributionMembers()).thenReturn(List.of(new ContributionMember(memberId,"m@example.test","Member")));
        service.create(actor,command(AssignmentMode.ALL_CURRENT,Set.of()));
        verify(repository).addAssignments(any(),eq(Set.of(memberId)));
        when(clubs.requireMembership(actor,clubId)).thenReturn(club());
        assertThrows(AccessDeniedException.class,()->service.create(actor,command(AssignmentMode.ALL_CURRENT,Set.of())));
    }
    @Test void selectedAssignmentRejectsUnavailableMemberAndPastOrRetrogradeRevision() {
        when(clubs.requireMembership(actor,clubId)).thenReturn(club("CONTRIBUTIONS_WRITE"));
        when(members.lockAndSnapshotActiveContributionMembers()).thenReturn(List.of(new ContributionMember(memberId,"m@example.test","Member")));
        assertThrows(AccessDeniedException.class,()->service.create(actor,command(AssignmentMode.SELECTED,Set.of(UUID.randomUUID()))));
        assertEquals(400,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.create(actor,
            new ContributionScheduleCommand("Monthly",BigDecimal.TEN,Frequency.MONTHLY,LocalDate.now(clock),null,LocalDate.now(clock).minusDays(1),AssignmentMode.ALL_CURRENT,Set.of()))).getStatusCode().value());
    }
    private ContributionScheduleCommand command(AssignmentMode mode,Set<UUID> selected){return new ContributionScheduleCommand("Monthly",new BigDecimal("100.00"),Frequency.MONTHLY,LocalDate.of(2026,9,1),null,LocalDate.of(2026,8,29),mode,selected);}
    private ClubSummary club(String...permissions){return new ClubSummary(clubId,"Club","INVESTMENT_CLUB",false,List.of(permissions));}
    private ContributionScheduleVersionEntity version(Frequency frequency,LocalDate due,LocalDate end,LocalDate effectiveFrom,LocalDate effectiveTo){
        var value=new ContributionScheduleVersionEntity(UUID.randomUUID(),UUID.randomUUID(),clubId,1,"Schedule",BigDecimal.TEN,frequency,due,end,effectiveFrom,AssignmentMode.ALL_CURRENT,actor,clock.instant());
        if(effectiveTo!=null)value.endOn(effectiveTo); return value;
    }
}
