package com.kds.backend.members.application;

import java.time.Instant;
import java.util.UUID;

public record MemberDirectoryEntry(UUID id, String email, String firstName, String lastName,
                                   String phone, String roleCode, MemberStatus status, Instant joinedOrInvitedAt) {
    public enum MemberStatus { INVITED, ACTIVE, SUSPENDED, EXITED }
}
