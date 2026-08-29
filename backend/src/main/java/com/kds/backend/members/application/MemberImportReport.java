package com.kds.backend.members.application;

import java.util.List;
import java.util.Map;

public final class MemberImportReport {
    private MemberImportReport() {}

    public record Inspection(List<String> headers, int rowCount, List<Map<String, String>> sampleRows) {}

    public record Preview(int totalRows, int readyRows, int invalidRows, List<Row> rows) {}

    public record Confirmation(int totalRows, int invitedRows, int failedRows, List<Row> rows) {}

    public record Row(int rowNumber, String email, String firstName, String lastName, String phone,
                      RowStatus status, List<String> errors) {
        public Row withStatus(RowStatus newStatus, List<String> newErrors) {
            return new Row(rowNumber, email, firstName, lastName, phone, newStatus, List.copyOf(newErrors));
        }
    }

    public enum RowStatus { READY, INVALID, INVITED, FAILED }
}
