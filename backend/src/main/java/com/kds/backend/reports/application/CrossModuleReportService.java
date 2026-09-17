package com.kds.backend.reports.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.meetings.application.MeetingService;
import com.kds.backend.members.application.MemberService;
import com.kds.backend.voting.application.MotionService;
import com.kds.backend.voting.domain.MotionState;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class CrossModuleReportService {
    private final ClubService clubs;
    private final MemberService members;
    private final MeetingService meetings;
    private final MotionService motions;
    private final Clock clock;

    public CrossModuleReportService(ClubService clubs, MemberService members, MeetingService meetings,
                                    MotionService motions, Clock clock) {
        this.clubs = clubs;
        this.members = members;
        this.meetings = meetings;
        this.motions = motions;
        this.clock = clock;
    }

    public ReportSnapshot snapshot(UUID actor, String kind, LocalDate from, LocalDate to) {
        var club = clubs.requireMembership(actor, TenantContext.requireClubId());
        if (!club.permissions().contains(Permission.REPORTS_READ.name())) {
            throw new AccessDeniedException("You do not have permission to export reports.");
        }
        if (from == null || to == null || from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid date range.");
        }
        return switch (kind.toUpperCase(java.util.Locale.ROOT)) {
            case "MEMBERS" -> new ReportSnapshot(club.name() + " - Member list",
                    List.of("Name", "Email", "Status", "Role", "Joined or invited"),
                    members.directory(actor, null, null).stream().map(member -> List.of(
                            String.join(" ", Stream.of(member.firstName(), member.lastName())
                                    .filter(value -> value != null && !value.isBlank()).toList()),
                            member.email(), member.status().name(), member.roleCode(),
                            member.joinedOrInvitedAt().toString())).toList(), clock.instant());
            case "MEETINGS" -> new ReportSnapshot(club.name() + " - Meeting history",
                    List.of("Title", "Date (UTC)", "Duration (minutes)", "Location"),
                    meetings.meetings(actor, MeetingService.View.PAST).stream()
                            .filter(meeting -> within(meeting.startsAt().toInstant().atZone(ZoneOffset.UTC).toLocalDate(), from, to))
                            .map(meeting -> List.of(meeting.title(), meeting.startsAt().toString(),
                                    Integer.toString(meeting.durationMinutes()),
                                    meeting.location() == null ? "" : meeting.location())).toList(), clock.instant());
            case "VOTING" -> new ReportSnapshot(club.name() + " - Voting history",
                    List.of("Motion", "Closed (UTC)", "State", "Results published"),
                    motions.all(actor).stream()
                            .filter(motion -> motion.state() == MotionState.CLOSED || motion.state() == MotionState.CANCELLED)
                            .filter(motion -> within(motion.closesAt().toInstant().atZone(ZoneOffset.UTC).toLocalDate(), from, to))
                            .map(motion -> List.of(motion.title(), motion.closesAt().toString(),
                                    motion.state().name(), motion.resultsPublished() ? "Yes" : "No")).toList(), clock.instant());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown report type.");
        };
    }

    private static boolean within(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
