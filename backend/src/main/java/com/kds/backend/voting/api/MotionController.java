package com.kds.backend.voting.api;

import com.kds.backend.voting.application.MotionCommand;
import com.kds.backend.voting.application.MotionService;
import com.kds.backend.voting.application.MotionView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/motions")
public class MotionController {
    private final MotionService service;
    public MotionController(MotionService service) { this.service = service; }

    public record Request(@PositiveOrZero long version, @NotBlank @Size(max = 200) String title,
                          @Size(max = 4000) String description, @NotNull OffsetDateTime opensAt,
                          @NotNull OffsetDateTime closesAt,
                          @NotNull @Size(min = 2, max = 20) List<@NotBlank @Size(max = 200) String> options,
                          Set<@NotNull UUID> eligibleMembershipIds, boolean allActiveMembers) {
        MotionCommand command() {
            return new MotionCommand(version, title, description, opensAt, closesAt, options,
                    eligibleMembershipIds, allActiveMembers);
        }
    }
    public record Version(@PositiveOrZero long version) {}

    @GetMapping
    @PreAuthorize("hasAuthority('VOTES_READ')")
    public List<MotionView> all(@AuthenticationPrincipal Jwt jwt) { return service.all(actor(jwt)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('VOTES_CREATE')")
    public MotionView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Request request) {
        return service.create(actor(jwt), request.command());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VOTES_CREATE')")
    public MotionView edit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody Request request) {
        return service.edit(actor(jwt), id, request.command());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('VOTES_CREATE')")
    public MotionView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody Version request) {
        return service.cancel(actor(jwt), id, request.version());
    }

    private static UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
