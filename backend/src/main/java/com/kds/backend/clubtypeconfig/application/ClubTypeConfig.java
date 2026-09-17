package com.kds.backend.clubtypeconfig.application;

import java.util.Set;

public record ClubTypeConfig(String code, String name, String administratorRoleCode,
                             String defaultMemberRoleCode, String memberLabel,
                             String contributionLabel, Set<String> enabledModules) {}
