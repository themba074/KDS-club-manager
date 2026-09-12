package com.kds.backend.voting.api;

import com.kds.backend.voting.application.MotionResultView;
import com.kds.backend.voting.application.VoteReceipt;
import com.kds.backend.voting.application.VoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/motions/{motionId}")
public class VoteController {
    private final VoteService service;
    public VoteController(VoteService service) { this.service = service; }

    public record CastVoteRequest(@NotNull UUID optionId) {}
    public record PublishResultRequest(@PositiveOrZero long version) {}

    @PostMapping("/votes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('VOTES_CAST')")
    public VoteReceipt cast(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID motionId,
                            @Valid @RequestBody CastVoteRequest request) {
        return service.cast(actor(jwt), motionId, request.optionId());
    }

    @GetMapping("/results")
    @PreAuthorize("hasAuthority('VOTES_READ')")
    public MotionResultView results(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID motionId) {
        return service.results(actor(jwt), motionId);
    }

    @PostMapping("/results/publish")
    @PreAuthorize("hasAuthority('VOTES_CREATE')")
    public MotionResultView publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID motionId,
                                    @Valid @RequestBody PublishResultRequest request) {
        return service.publish(actor(jwt), motionId, request.version());
    }

    private static UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
