package com.kds.backend.identity.application;

import java.util.UUID;

public record MembershipOnboardingResult(UUID userId, UUID membershipId, boolean accountCreated) {}
