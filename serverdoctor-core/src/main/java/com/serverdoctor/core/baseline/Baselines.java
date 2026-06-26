package com.serverdoctor.core.baseline;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Severity;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/** Builds a {@link Baseline} from a {@link DiagnosticReport}. */
public final class Baselines {
    private Baselines() {}

    public static Baseline from(DiagnosticReport r, String serverVersion) {
        PerformanceSnapshot p = r.performance();
        Map<Severity, Integer> bySev = new EnumMap<>(Severity.class);
        for (AnalysisResult res : r.results())
            for (Finding f : res.findings())
                bySev.merge(f.severity(), 1, Integer::sum);
        return new Baseline(
                Instant.now(),
                serverVersion == null ? "" : serverVersion,
                p == null ? Double.NaN : p.tps1m(),
                p == null ? Double.NaN : p.mspt(),
                p == null ? 0L : p.memory().usedMb(),
                r.conflicts().size(),
                r.securityRisks().size(),
                r.recommendations().size(),
                bySev);
    }
}
