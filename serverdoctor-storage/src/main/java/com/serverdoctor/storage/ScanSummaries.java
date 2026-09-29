package com.serverdoctor.storage;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.ScanSummary;
import com.serverdoctor.common.model.Severity;

/** Verdichtet einen {@link DiagnosticReport} zu einer {@link ScanSummary}. */
public final class ScanSummaries {

    /**
     * Findings dieses Scanners werden nicht mitgezählt: Er meldet selbst Count-Trends,
     * seine eigenen Findings würden den Trend sonst künstlich verstärken.
     */
    static final String REGRESSION_SCANNER_ID = "regression";

    private ScanSummaries() {}

    public static ScanSummary of(DiagnosticReport report) {
        int findings = 0;
        int severe = 0;
        for (AnalysisResult result : report.results()) {
            if (REGRESSION_SCANNER_ID.equals(result.moduleId())) continue;
            for (Finding f : result.findings()) {
                findings++;
                if (f.severity().atLeast(Severity.HIGH)) severe++;
            }
        }
        return new ScanSummary(report.timestamp(), findings, severe,
                report.conflicts().size(), report.securityRisks().size());
    }
}
