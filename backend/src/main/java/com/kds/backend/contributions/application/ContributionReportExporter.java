package com.kds.backend.contributions.application;
import java.io.*;
public interface ContributionReportExporter {
    ContributionReportFormat format();
    String mediaType();
    String extension();
    void write(ContributionReport report,OutputStream output) throws IOException;
}
