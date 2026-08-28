package com.kds.backend.members.application;

import java.time.Instant;

public record InvitationPreview(String clubName, String email, String firstName, String lastName,
                                String roleCode, boolean accountExists, Instant expiresAt) {}
