package com.kds.backend.members.application;

import com.kds.backend.identity.application.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.kds.backend.members.application.MemberImportReport.RowStatus.FAILED;
import static com.kds.backend.members.application.MemberImportReport.RowStatus.INVALID;
import static com.kds.backend.members.application.MemberImportReport.RowStatus.INVITED;

@Service
public class MemberImportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemberImportService.class);
    private final MemberCsvParser parser;
    private final MemberImportPreviewService previews;
    private final MemberService members;

    public MemberImportService(MemberCsvParser parser, MemberImportPreviewService previews, MemberService members) {
        this.parser = parser;
        this.previews = previews;
        this.members = members;
    }

    public MemberImportReport.Inspection inspect(UUID actor, MultipartFile file) {
        previews.requireWriteAccess(actor);
        MemberCsvParser.ParsedCsv csv = parser.parse(file);
        var samples = csv.rows().stream().limit(MemberCsvParser.SAMPLE_ROWS)
                .map(row -> row.values()).toList();
        return new MemberImportReport.Inspection(csv.headers(), csv.rows().size(), samples);
    }

    public MemberImportReport.Preview preview(UUID actor, MultipartFile file, MemberImportMapping mapping) {
        return previews.preview(actor, file, mapping);
    }

    public MemberImportReport.Confirmation confirm(UUID actor, MultipartFile file, MemberImportMapping mapping) {
        MemberImportReport.Preview preview = previews.preview(actor, file, mapping);
        List<MemberImportReport.Row> results = new ArrayList<>(preview.totalRows());
        int invited = 0;
        for (MemberImportReport.Row row : preview.rows()) {
            if (row.status() == INVALID) {
                results.add(row);
                continue;
            }
            try {
                members.invite(actor, row.email(), row.firstName(), row.lastName(), row.phone());
                results.add(row.withStatus(INVITED, List.of()));
                invited++;
            } catch (ResponseStatusException exception) {
                String message = exception.getReason() == null ? "The invitation could not be created." : exception.getReason();
                results.add(row.withStatus(FAILED, List.of(message)));
            } catch (RuntimeException exception) {
                LOGGER.warn("member_import_row_failed clubId={} actorId={} rowNumber={}",
                        TenantContext.requireClubId(), actor, row.rowNumber(), exception);
                results.add(row.withStatus(FAILED, List.of("The invitation could not be created.")));
            }
        }
        LOGGER.info("member_import_confirmed clubId={} actorId={} totalRows={} invitedRows={} failedRows={}",
                TenantContext.requireClubId(), actor, results.size(), invited, results.size() - invited);
        return new MemberImportReport.Confirmation(results.size(), invited, results.size() - invited, results);
    }
}
