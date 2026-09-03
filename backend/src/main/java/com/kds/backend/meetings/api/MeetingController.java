package com.kds.backend.meetings.api;
import com.kds.backend.meetings.application.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/meetings")
public class MeetingController {
    private final MeetingService service;
    public MeetingController(MeetingService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('MEETINGS_READ')")
    public List<MeetingView> meetings(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="UPCOMING") MeetingService.View view){return service.meetings(actor(jwt),view);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('MEETINGS_WRITE')")
    public MeetingView create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody MeetingDtos.MeetingRequest request){return service.create(actor(jwt),request.command());}
    @PutMapping("/{meetingId}") @PreAuthorize("hasAuthority('MEETINGS_WRITE')")
    public MeetingView edit(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID meetingId,@Valid @RequestBody MeetingDtos.MeetingRequest request){return service.edit(actor(jwt),meetingId,request.command());}
    private UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
