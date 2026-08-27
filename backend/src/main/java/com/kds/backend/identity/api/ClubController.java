package com.kds.backend.identity.api;

import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ClubController {
    private final ClubService clubs;
    public ClubController(ClubService clubs) { this.clubs = clubs; }

    public record CreateClubRequest(@NotBlank @Size(max = 120) String name,
                                   @NotBlank @Pattern(regexp = "INVESTMENT_CLUB") String clubType) {}

    @PostMapping("/clubs")
    @ResponseStatus(HttpStatus.CREATED)
    public ClubSummary create(@AuthenticationPrincipal Jwt principal, @Valid @RequestBody CreateClubRequest request) {
        return clubs.create(UUID.fromString(principal.getSubject()), request.name());
    }

    @GetMapping("/clubs")
    public List<ClubSummary> list(@AuthenticationPrincipal Jwt principal) {
        return clubs.listForUser(UUID.fromString(principal.getSubject()));
    }

    @GetMapping("/club")
    public ClubSummary current(@AuthenticationPrincipal Jwt principal) {
        return clubs.current(UUID.fromString(principal.getSubject()));
    }
}
