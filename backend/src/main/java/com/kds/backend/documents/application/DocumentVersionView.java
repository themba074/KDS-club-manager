package com.kds.backend.documents.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentVersionView(UUID id, int versionNumber, String fileName, String contentType,
                                  long fileSize, UUID uploadedBy, OffsetDateTime uploadedAt) {}
