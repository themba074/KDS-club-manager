package com.kds.backend.meetings.api;

import com.kds.backend.meetings.application.*;
import com.kds.backend.meetings.domain.MeetingRsvpEntity.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/meetings/{meetingId}/rsvp")
public class RsvpController {
    private final RsvpService service;
    public RsvpController(RsvpService service){this.service=service;}
    public record Request(@NotNull Response response) {}
    @GetMapping @PreAuthorize("hasAuthority('MEETINGS_READ')") public RsvpView view(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID meetingId){return service.view(actor(jwt),meetingId);}
    @PutMapping @PreAuthorize("hasAuthority('MEETINGS_READ')") public RsvpView respond(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID meetingId,@Valid @RequestBody Request request){return service.respond(actor(jwt),meetingId,request.response());}
    private static UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
