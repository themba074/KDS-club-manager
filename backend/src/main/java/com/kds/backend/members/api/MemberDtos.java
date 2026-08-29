package com.kds.backend.members.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.kds.backend.members.application.MemberDirectoryEntry;

public final class MemberDtos {
    private MemberDtos() {}

    public record InviteMemberRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @Size(max = 30) String phone) {}

    public record AcceptInvitationRequest(
            @NotBlank String token,
            @Size(min = 8, max = 72) String password) {}

    public record ChangeMemberStatusRequest(@NotNull MemberDirectoryEntry.MemberStatus status) {}
}
