package com.kds.backend.identity.application;

import java.util.UUID;

public record ClubSummary(UUID id, String name, String clubType, boolean administrator) {}
