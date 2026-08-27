package com.kds.backend.clubtypeconfig.application;
import java.util.Set;
public record RoleDefinition(String code, String name, Set<Permission> permissions) {}

