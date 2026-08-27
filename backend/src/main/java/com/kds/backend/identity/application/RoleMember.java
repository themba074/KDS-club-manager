package com.kds.backend.identity.application;
import java.util.UUID;
public record RoleMember(UUID id, String email, String roleCode) {}

