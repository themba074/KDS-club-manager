package com.kds.backend.identity.application;

import java.util.UUID;

public record MembershipLifecycleMember(UUID membershipId, UUID userId, String roleCode, String status) {}
