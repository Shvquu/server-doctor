package com.serverdoctor.api.module;

import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;

import java.time.Instant;
import java.util.List;

/** Aggregiertes Ergebnis eines kompletten Analyse-Laufs. */
public record DiagnosticReport(Instant timestamp,
                               PerformanceSnapshot performance,
                               List<AnalysisResult> results,
                               List<Recommendation> recommendations) {

    public DiagnosticReport {
        results         = results         == null ? List.of() : List.copyOf(results);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }

    public List<ConflictReport> conflicts() {
        return results.stream().flatMap(r -> r.conflicts().stream()).toList();
    }

    public List<SecurityRisk> securityRisks() {
        return results.stream().flatMap(r -> r.securityRisks().stream()).toList();
    }

    public Severity overallSeverity() {
        return results.stream().map(AnalysisResult::severity)
                .reduce(Severity.OK, Severity::max);
    }
}
