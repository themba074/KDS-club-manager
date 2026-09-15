package com.kds.backend.audit.application;

import java.util.List;

public record AuditLogPage(List<AuditLogView> content, long totalElements, int totalPages, int page, int size) {}
