package com.kds.backend.documents.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record DocumentView(UUID id, String title, String category, Set<String> visibleRoleCodes,
                           UUID createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                           long version, List<DocumentVersionView> versions) {}
