package com.kds.backend.contributions.infrastructure;

import com.kds.backend.contributions.application.*;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ContributionReportExporterTests {
    @Test void csvUsesExactDecimalsAndNeutralizesSpreadsheetFormulas()throws Exception{
        ByteArrayOutputStream output=new ByteArrayOutputStream();new ContributionCsvExporter().write(report(List.of(member(UUID.randomUUID(),"=HYPERLINK(\"bad\")","10.10","4.05"))),output);
        String csv=output.toString(StandardCharsets.UTF_8);assertTrue(csv.contains("'=HYPERLINK"));assertTrue(csv.contains("10.10,4.05,6.05,ZAR"));assertTrue(csv.contains("TOTAL,,10.10,4.05,6.05,ZAR"));
    }
    @Test void pdfStartsWithPdfSignatureAndPaginatesHundredsReadyLayout()throws Exception{
        List<MemberContributionSummary> members=new ArrayList<>();for(int index=0;index<100;index++)members.add(member(UUID.randomUUID(),"Member "+index,"10.00","5.00"));
        ByteArrayOutputStream output=new ByteArrayOutputStream();new ContributionPdfExporter().write(report(members),output);byte[] bytes=output.toByteArray();
        assertEquals("%PDF",new String(bytes,0,4,StandardCharsets.US_ASCII));try(var document=Loader.loadPDF(bytes)){assertTrue(document.getNumberOfPages()>=3);}
    }
    private ContributionReport report(List<MemberContributionSummary> members){BigDecimal expected=members.stream().map(MemberContributionSummary::expected).reduce(BigDecimal.ZERO,BigDecimal::add),collected=members.stream().map(MemberContributionSummary::collected).reduce(BigDecimal.ZERO,BigDecimal::add),outstanding=members.stream().map(MemberContributionSummary::outstanding).reduce(BigDecimal.ZERO,BigDecimal::add);return new ContributionReport(UUID.randomUUID(),"Ubuntu Club",LocalDate.of(2026,1,1),LocalDate.of(2026,12,31),Instant.parse("2026-09-03T08:00:00Z"),expected,collected,outstanding,"ZAR",members);}
    private MemberContributionSummary member(UUID id,String name,String expected,String collected){BigDecimal expectedValue=new BigDecimal(expected),collectedValue=new BigDecimal(collected);return new MemberContributionSummary(id,name,"member@example.test",expectedValue,collectedValue,expectedValue.subtract(collectedValue).max(BigDecimal.ZERO),"ZAR");}
}
