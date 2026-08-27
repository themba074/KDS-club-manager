package com.kds.backend.identity.application;

import java.util.UUID;

public record TokenPair(String accessToken, String refreshToken, long accessTokenExpiresInSeconds,
                        UUID userId, String email, ClubSummary activeClub) {}
