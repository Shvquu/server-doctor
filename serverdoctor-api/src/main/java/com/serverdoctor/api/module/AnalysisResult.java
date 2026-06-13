package com.serverdoctor.api.module;

import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;

import java.util.List;

/** Immutables Ergebnis eines Analyse-Moduls. */
public record AnalysisResult(String moduleId, Severity severity,
                             List<Finding> findings,
                             List<ConflictReport> conflicts,
                             List<SecurityRisk> securityRisks) {

    public AnalysisResult {
        findings      = findings      == null ? List.of() : List.copyOf(findings);
        conflicts     = conflicts     == null ? List.of() : List.copyOf(conflicts);
        securityRisks = securityRisks == null ? List.of() : List.copyOf(securityRisks);
    }

    public static AnalysisResult empty(String moduleId) {
        return new AnalysisResult(moduleId, Severity.OK, List.of(), List.of(), List.of());
    }

    public static Builder builder(String moduleId) { return new Builder(moduleId); }

    public static final class Builder {
        private final String moduleId;
        private Severity severity = Severity.OK;
        private final java.util.List<Finding> findings = new java.util.ArrayList<>();
        private final java.util.List<ConflictReport> conflicts = new java.util.ArrayList<>();
        private final java.util.List<SecurityRisk> risks = new java.util.ArrayList<>();

        private Builder(String moduleId) { this.moduleId = moduleId; }

        public Builder finding(Finding f) { findings.add(f); bump(f.severity()); return this; }
        public Builder conflict(ConflictReport c) { conflicts.add(c); bump(c.severity()); return this; }
        public Builder risk(SecurityRisk r) { risks.add(r); bump(r.severity()); return this; }

        private void bump(Severity s) { severity = Severity.max(severity, s); }

        public AnalysisResult build() {
            return new AnalysisResult(moduleId, severity, findings, conflicts, risks);
        }
    }
}
