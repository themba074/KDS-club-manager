package com.kds.backend.documents.application;

import java.util.Set;

public record DocumentCommand(long version, String title, String category, Set<String> visibleRoleCodes) {}
