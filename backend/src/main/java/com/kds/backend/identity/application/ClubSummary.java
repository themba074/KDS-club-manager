package com.kds.backend.identity.application;

import java.util.UUID;

public record ClubSummary(UUID id, String name, String clubType, boolean administrator, java.util.List<String> permissions) {
    public ClubSummary(UUID id, String name, String clubType, boolean administrator) {
        this(id, name, clubType, administrator, java.util.List.of());
    }
}
