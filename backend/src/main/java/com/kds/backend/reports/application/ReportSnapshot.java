package com.kds.backend.reports.application;

import java.time.Instant;
import java.util.List;

/** One authorized, tenant-scoped snapshot shared by CSV and PDF rendering. */
public record ReportSnapshot(String title, List<String> headers, List<List<String>> rows, Instant generatedAt) {}
