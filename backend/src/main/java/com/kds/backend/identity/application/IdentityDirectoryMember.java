package com.kds.backend.identity.application;

import java.time.Instant;
import java.util.UUID;

public record IdentityDirectoryMember(UUID membershipId, String email, String roleCode, String status, Instant joinedAt) {}
