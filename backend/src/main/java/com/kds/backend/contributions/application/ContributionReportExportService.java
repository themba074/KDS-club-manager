package com.kds.backend.contributions.application;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContributionReportExportService {
    private final Map<ContributionReportFormat,ContributionReportExporter> exporters;
    public ContributionReportExportService(List<ContributionReportExporter> exporters){this.exporters=exporters.stream().collect(Collectors.toUnmodifiableMap(ContributionReportExporter::format,Function.identity()));}
    public ContributionReportExporter require(ContributionReportFormat format){return Optional.ofNullable(exporters.get(format)).orElseThrow(()->new IllegalArgumentException("Unsupported report format."));}
}
