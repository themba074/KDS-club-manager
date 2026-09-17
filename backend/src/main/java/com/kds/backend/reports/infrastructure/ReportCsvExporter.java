package com.kds.backend.reports.infrastructure;

import com.kds.backend.reports.application.ReportSnapshot;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Component
public class ReportCsvExporter {
    public void write(ReportSnapshot snapshot, OutputStream output) throws IOException {
        var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write('\ufeff');
        try (var csv = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader(snapshot.headers().toArray(String[]::new)).get())) {
            for (var row : snapshot.rows()) csv.printRecord(row.stream().map(ReportCsvExporter::safe).toList());
        }
    }

    private static String safe(String value) {
        if (value == null) return "";
        String clean = value.replace('\r', ' ').replace('\n', ' ');
        return !clean.isEmpty() && "=+-@".indexOf(clean.charAt(0)) >= 0 ? "'" + clean : clean;
    }
}
