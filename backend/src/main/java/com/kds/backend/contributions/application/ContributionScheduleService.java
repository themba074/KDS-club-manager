package com.kds.backend.contributions.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.contributions.domain.*;
import com.kds.backend.contributions.domain.ContributionScheduleVersionEntity.*;
import com.kds.backend.contributions.repository.ContributionScheduleRepository;
import com.kds.backend.identity.application.*;
import com.kds.backend.members.application.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @Transactional(readOnly=true)
public class ContributionScheduleService {
    private final ContributionScheduleRepository schedules;
    private final MemberService members;
    private final ClubService clubs;
    private final ContributionScheduleMapper mapper;
    private final Clock clock;
    public ContributionScheduleService(ContributionScheduleRepository schedules,MemberService members,ClubService clubs,
            ContributionScheduleMapper mapper,Clock clock) {
        this.schedules=schedules; this.members=members; this.clubs=clubs; this.mapper=mapper; this.clock=clock;
    }
    public List<ContributionMember> assignableMembers(UUID actor) { require(actor,Permission.CONTRIBUTIONS_WRITE); return members.activeContributionMembers(); }
    public List<ContributionScheduleView> schedules(UUID actor) {
        require(actor,Permission.CONTRIBUTIONS_READ);
        Map<UUID,ContributionMember> directory=memberDirectory();
        return schedules.latestVersions().stream().map(v->view(v,directory)).toList();
    }
    @Transactional
    public ContributionScheduleView create(UUID actor,ContributionScheduleCommand command) {
        require(actor,Permission.CONTRIBUTIONS_WRITE); validate(command,true,null);
        List<ContributionMember> active=members.lockAndSnapshotActiveContributionMembers(); require(actor,Permission.CONTRIBUTIONS_WRITE);
        Set<UUID> assigned=assignments(command,active);
        UUID club=TenantContext.requireClubId(),scheduleId=UUID.randomUUID(); Instant now=clock.instant();
        schedules.add(new ContributionScheduleEntity(scheduleId,club,actor,now));
        var version=version(scheduleId,club,1,actor,now,command,assigned); schedules.add(version); schedules.addAssignments(version.getId(),assigned);
        return view(version,memberDirectory());
    }
    @Transactional
    public ContributionScheduleView revise(UUID actor,UUID scheduleId,ContributionScheduleCommand command) {
        require(actor,Permission.CONTRIBUTIONS_WRITE);
        List<ContributionMember> active=members.lockAndSnapshotActiveContributionMembers(); require(actor,Permission.CONTRIBUTIONS_WRITE);
        schedules.lockSchedule(scheduleId).orElseThrow(()->new AccessDeniedException("Schedule is unavailable in this club."));
        var latest=schedules.latest(scheduleId).orElseThrow();
        validate(command,false,latest);
        Set<UUID> assigned=assignments(command,active);
        latest.endOn(command.effectiveFrom().minusDays(1));
        var revision=version(scheduleId,TenantContext.requireClubId(),latest.getVersionNumber()+1,actor,clock.instant(),command,assigned);
        schedules.add(revision); schedules.addAssignments(revision.getId(),assigned); return view(revision,memberDirectory());
    }
    public List<ExpectedContribution> upcoming(UUID actor,LocalDate from,LocalDate to) {
        require(actor,Permission.CONTRIBUTIONS_READ);
        if (from==null || to==null || to.isBefore(from) || to.isAfter(from.plusYears(1)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a valid date range of at most one year.");
        Map<UUID,ContributionMember> directory=memberDirectory();
        List<ExpectedContribution> result=new ArrayList<>();
        for(var version:schedules.versionsOverlapping(from,to)) for(LocalDate due:dueDates(version,from,to))
            for(UUID memberId:schedules.assignments(version.getId())) {
                ContributionMember member=directory.get(memberId);
                // Historical assignments may reference a member who is no longer active; preserve the expectation without leaking another club.
                String email=member==null ? "Former member" : member.email(); String name=member==null ? "Former member" : member.displayName();
                result.add(new ExpectedContribution(version.getScheduleId(),version.getId(),version.getName(),memberId,email,name,due,version.getAmount(),version.getCurrency()));
            }
        return result.stream().sorted(Comparator.comparing(ExpectedContribution::dueDate).thenComparing(ExpectedContribution::scheduleName).thenComparing(ExpectedContribution::memberName)).toList();
    }
    public static List<LocalDate> dueDates(ContributionScheduleVersionEntity version,LocalDate from,LocalDate to) {
        LocalDate lower=max(from,version.getFirstDueDate(),version.getEffectiveFrom());
        LocalDate upper=min(to,version.getEndDate(),version.getEffectiveTo());
        if (upper.isBefore(lower)) return List.of();
        if (version.getFrequency()==Frequency.ONCE_OFF)
            return !version.getFirstDueDate().isBefore(lower)&&!version.getFirstDueDate().isAfter(upper)?List.of(version.getFirstDueDate()):List.of();
        int day=version.getFirstDueDate().getDayOfMonth(); YearMonth month=YearMonth.from(lower); List<LocalDate> dates=new ArrayList<>();
        while(!month.atDay(1).isAfter(upper)) {
            LocalDate due=month.atDay(Math.min(day,month.lengthOfMonth()));
            if(!due.isBefore(lower)&&!due.isAfter(upper)) dates.add(due);
            month=month.plusMonths(1);
        }
        return dates;
    }
    private void validate(ContributionScheduleCommand command,boolean creating,ContributionScheduleVersionEntity latest) {
        if(command.endDate()!=null&&command.endDate().isBefore(command.firstDueDate())) throw bad("End date cannot be before the first due date.");
        if(command.frequency()==Frequency.ONCE_OFF&&command.endDate()!=null) throw bad("A once-off schedule does not need an end date.");
        if(command.effectiveFrom().isBefore(LocalDate.now(clock))) throw bad("Schedule changes cannot take effect in the past.");
        if(creating&&command.firstDueDate().isBefore(command.effectiveFrom())) throw bad("First due date cannot be before the schedule starts.");
        if(!creating&&!command.effectiveFrom().isAfter(latest.getEffectiveFrom())) throw bad("A revision must take effect after the current revision.");
    }
    private Set<UUID> assignments(ContributionScheduleCommand command,List<ContributionMember> snapshot) {
        Set<UUID> active=snapshot.stream().map(ContributionMember::membershipId).collect(Collectors.toSet());
        Set<UUID> selected=command.assignmentMode()==AssignmentMode.ALL_CURRENT?active:new LinkedHashSet<>(command.membershipIds());
        if(selected.isEmpty()) throw bad("Assign at least one active member.");
        if(!active.containsAll(selected)) throw new AccessDeniedException("One or more selected members are unavailable in this club.");
        return selected;
    }
    private ContributionScheduleVersionEntity version(UUID schedule,UUID club,int number,UUID actor,Instant now,ContributionScheduleCommand c,Set<UUID> assigned) {
        return new ContributionScheduleVersionEntity(UUID.randomUUID(),schedule,club,number,c.name().strip(),c.amount(),c.frequency(),c.firstDueDate(),c.endDate(),c.effectiveFrom(),c.assignmentMode(),actor,now);
    }
    private ContributionScheduleView view(ContributionScheduleVersionEntity v,Map<UUID,ContributionMember> directory) {
        return mapper.view(v,schedules.assignments(v.getId()).stream().map(directory::get).filter(Objects::nonNull).sorted(Comparator.comparing(ContributionMember::displayName)).toList());
    }
    private Map<UUID,ContributionMember> memberDirectory(){return members.contributionMembers().stream().collect(Collectors.toMap(ContributionMember::membershipId,Function.identity()));}
    private void require(UUID actor,Permission permission){if(!clubs.requireMembership(actor,TenantContext.requireClubId()).permissions().contains(permission.name()))throw new AccessDeniedException("You do not have permission for this action.");}
    private static LocalDate max(LocalDate...values){return Arrays.stream(values).filter(Objects::nonNull).max(LocalDate::compareTo).orElseThrow();}
    private static LocalDate min(LocalDate required,LocalDate...optional){return Arrays.stream(optional).filter(Objects::nonNull).reduce(required,(a,b)->a.isBefore(b)?a:b);}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
