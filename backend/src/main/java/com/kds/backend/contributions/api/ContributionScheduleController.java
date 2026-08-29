package com.kds.backend.contributions.api;
import com.kds.backend.contributions.application.*;
import com.kds.backend.members.application.ContributionMember;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/api/v1/contribution-schedules")
public class ContributionScheduleController {
    private final ContributionScheduleService service;
    public ContributionScheduleController(ContributionScheduleService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('CONTRIBUTIONS_READ')")
    public List<ContributionScheduleView> schedules(@AuthenticationPrincipal Jwt jwt){return service.schedules(actor(jwt));}
    @GetMapping("/assignable-members") @PreAuthorize("hasAuthority('CONTRIBUTIONS_WRITE')")
    public List<ContributionMember> members(@AuthenticationPrincipal Jwt jwt){return service.assignableMembers(actor(jwt));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('CONTRIBUTIONS_WRITE')")
    public ContributionScheduleView create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ContributionScheduleDtos.ScheduleRequest request){return service.create(actor(jwt),request.command());}
    @PutMapping("/{scheduleId}") @PreAuthorize("hasAuthority('CONTRIBUTIONS_WRITE')")
    public ContributionScheduleView revise(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID scheduleId,@Valid @RequestBody ContributionScheduleDtos.ScheduleRequest request){return service.revise(actor(jwt),scheduleId,request.command());}
    @GetMapping("/upcoming") @PreAuthorize("hasAuthority('CONTRIBUTIONS_READ')")
    public List<ExpectedContribution> upcoming(@AuthenticationPrincipal Jwt jwt,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return service.upcoming(actor(jwt),from,to);}
    private UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
