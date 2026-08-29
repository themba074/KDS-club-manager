package com.kds.backend.members.application;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MemberCsvParser {
    static final long MAX_FILE_BYTES = 1_000_000;
    static final int MAX_ROWS = 1_000;
    static final int MAX_COLUMNS = 50;
    static final int SAMPLE_ROWS = 5;

    public ParsedCsv parse(MultipartFile file) {
        validateFile(file);
        CSVFormat format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .get();
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser csv = format.parse(reader)) {
            List<String> headers = cleanHeaders(csv.getHeaderNames());
            List<ParsedRow> rows = new ArrayList<>();
            for (CSVRecord record : csv) {
                if (rows.size() >= MAX_ROWS) {
                    throw badRequest("CSV files may contain at most " + MAX_ROWS + " member rows.");
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (int index = 0; index < headers.size(); index++) {
                    values.put(headers.get(index), index < record.size() ? record.get(index).strip() : "");
                }
                rows.add(new ParsedRow(Math.toIntExact(record.getRecordNumber() + 1), values));
            }
            if (rows.isEmpty()) throw badRequest("The CSV file does not contain any member rows.");
            return new ParsedCsv(headers, rows);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw badRequest("The CSV file could not be read. Check its headers, quotes, and delimiters.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw badRequest("Choose a non-empty CSV file.");
        if (file.getSize() > MAX_FILE_BYTES) throw badRequest("CSV files must be 1 MB or smaller.");
    }

    private List<String> cleanHeaders(List<String> rawHeaders) {
        if (rawHeaders.isEmpty()) throw badRequest("The CSV file must include a header row.");
        if (rawHeaders.size() > MAX_COLUMNS) {
            throw badRequest("CSV files may contain at most " + MAX_COLUMNS + " columns.");
        }
        List<String> headers = new ArrayList<>(rawHeaders.size());
        for (int index = 0; index < rawHeaders.size(); index++) {
            String header = rawHeaders.get(index).strip();
            if (index == 0 && header.startsWith("\uFEFF")) header = header.substring(1).strip();
            if (header.isBlank()) throw badRequest("Every CSV column must have a header.");
            if (headers.contains(header)) throw badRequest("CSV headers must be unique.");
            headers.add(header);
        }
        return List.copyOf(headers);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record ParsedCsv(List<String> headers, List<ParsedRow> rows) {}
    public record ParsedRow(int rowNumber, Map<String, String> values) {}
}
