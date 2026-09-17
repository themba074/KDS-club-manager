package com.kds.backend.reports.api;

import com.kds.backend.reports.application.CrossModuleReportService;
import com.kds.backend.reports.application.ReportFormat;
import com.kds.backend.reports.infrastructure.ReportCsvExporter;
import com.kds.backend.reports.infrastructure.ReportPdfExporter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final CrossModuleReportService reports;
    private final ReportCsvExporter csv;
    private final ReportPdfExporter pdf;

    public ReportController(CrossModuleReportService reports, ReportCsvExporter csv, ReportPdfExporter pdf) {
        this.reports = reports;
        this.csv = csv;
        this.pdf = pdf;
    }

    @GetMapping("/{kind}/export")
    @PreAuthorize("hasAuthority('REPORTS_READ')")
    public ResponseEntity<StreamingResponseBody> export(@AuthenticationPrincipal Jwt jwt, @PathVariable String kind,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam ReportFormat format) {
        var snapshot = reports.snapshot(UUID.fromString(jwt.getSubject()), kind, from, to);
        String extension = format.name().toLowerCase(Locale.ROOT);
        String filename = kind.toLowerCase(Locale.ROOT) + "-" + from + "-to-" + to + "." + extension;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(format == ReportFormat.CSV ? MediaType.parseMediaType("text/csv;charset=UTF-8") : MediaType.APPLICATION_PDF)
                .body(output -> {
                    if (format == ReportFormat.CSV) csv.write(snapshot, output);
                    else pdf.write(snapshot, output);
                });
    }
}
