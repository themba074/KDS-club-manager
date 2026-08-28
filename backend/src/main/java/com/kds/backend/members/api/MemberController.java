package com.kds.backend.members.api;

import com.kds.backend.identity.api.AuthDtos.AuthResponse;
import com.kds.backend.identity.api.AuthSessionResponder;
import com.kds.backend.members.application.InvitationPreview;
import com.kds.backend.members.application.MemberDirectoryEntry;
import com.kds.backend.members.application.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.kds.backend.members.api.MemberDtos.AcceptInvitationRequest;
import static com.kds.backend.members.api.MemberDtos.InviteMemberRequest;

@RestController
@RequestMapping("/api/v1")
public class MemberController {
    private final MemberService members;
    private final AuthSessionResponder sessions;

    public MemberController(MemberService members, AuthSessionResponder sessions) {
        this.members = members;
        this.sessions = sessions;
    }

    @GetMapping("/members")
    @PreAuthorize("hasAuthority('MEMBERS_READ')")
    public List<MemberDirectoryEntry> directory(@AuthenticationPrincipal Jwt principal,
                                                 @RequestParam(required = false) String search,
                                                 @RequestParam(required = false) MemberDirectoryEntry.MemberStatus status) {
        return members.directory(UUID.fromString(principal.getSubject()), search, status);
    }

    @PostMapping("/member-invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MEMBERS_WRITE')")
    public MemberDirectoryEntry invite(@AuthenticationPrincipal Jwt principal,
                                       @Valid @RequestBody InviteMemberRequest request) {
        return members.invite(UUID.fromString(principal.getSubject()), request.email(), request.firstName(),
                request.lastName(), request.phone());
    }

    @GetMapping("/member-invitations/accept")
    public InvitationPreview preview(@RequestParam String token) { return members.preview(token); }

    @PostMapping("/member-invitations/accept")
    public AuthResponse accept(@Valid @RequestBody AcceptInvitationRequest request, HttpServletResponse response) {
        return sessions.respond(members.accept(request.token(), request.password()), response);
    }
}
