package com.kds.backend.contributions.infrastructure;

import com.kds.backend.contributions.application.*;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Component;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Component
public class ContributionCsvExporter implements ContributionReportExporter {
    @Override public ContributionReportFormat format(){return ContributionReportFormat.CSV;}
    @Override public String mediaType(){return "text/csv;charset=UTF-8";}
    @Override public String extension(){return "csv";}
    @Override public void write(ContributionReport report,OutputStream output)throws IOException{
        Writer writer=new OutputStreamWriter(output,StandardCharsets.UTF_8);writer.write('\ufeff');
        CSVFormat format=CSVFormat.DEFAULT.builder().setHeader("Member","Email","Expected","Collected","Outstanding","Currency").get();
        try(CSVPrinter csv=new CSVPrinter(writer,format)){
            for(MemberContributionSummary member:report.members())csv.printRecord(safe(member.memberName()),safe(member.memberEmail()),money(member.expected()),money(member.collected()),money(member.outstanding()),member.currency());
            csv.printRecord("TOTAL","",money(report.totalExpected()),money(report.totalCollected()),money(report.totalOutstanding()),report.currency());
        }
    }
    static String safe(String value){if(value==null)return "";String clean=value.replace('\r',' ').replace('\n',' ');return !clean.isEmpty()&&"=+-@".indexOf(clean.charAt(0))>=0?"'"+clean:clean;}
    private static String money(BigDecimal value){return value.setScale(2).toPlainString();}
}
