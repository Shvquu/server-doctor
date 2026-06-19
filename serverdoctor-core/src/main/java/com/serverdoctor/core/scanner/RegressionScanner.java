package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.regression.NoopPerformanceHistory;
import com.serverdoctor.core.regression.PerformanceHistory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * Detects slow performance regressions by comparing the older half of stored snapshots against
 * the newer half (TPS, MSPT, RAM). Because it works on the retained window rather than a single
 * reading, it catches gradual decline over days/weeks - not just momentary spikes.
 *
 * <p>Uses only data that is already persisted, via an injected {@link PerformanceHistory}
 * (backed by storage in each platform adapter). Runs on Paper/Folia, Velocity and BungeeCord;
 * on proxies TPS/MSPT are NaN and are skipped automatically, while RAM is still compared.
 */
public final class RegressionScanner implements AnalysisModule {

    private final PerformanceHistory history;

    // thresholds (percent change required before reporting)
    private final int window = 500;
    private final int minSamples = 8;
    private final double tpsDropPct = 8.0;
    private final double msptRisePct = 25.0;
    private final double ramRisePct = 30.0;

    public RegressionScanner() {
        this(NoopPerformanceHistory.INSTANCE);
    }

    public RegressionScanner(PerformanceHistory history) {
        this.history = history == null ? NoopPerformanceHistory.INSTANCE : history;
    }

    @Override public String id() { return "regression"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());

        List<PerformanceSnapshot> hist = history.recent(window);
        if (hist == null || hist.size() < minSamples) return out.build();

        List<PerformanceSnapshot> sorted = new ArrayList<>(hist);
        sorted.sort(Comparator.comparing(PerformanceSnapshot::capturedAt).reversed()); // newest first

        int mid = sorted.size() / 2;
        List<PerformanceSnapshot> newer = sorted.subList(0, mid);
        List<PerformanceSnapshot> older = sorted.subList(mid, sorted.size());

        List<String> parts = new ArrayList<>();
        Severity severity = Severity.OK;

        // TPS - lower is worse
        Double tNew = avg(newer, PerformanceSnapshot::tps1m);
        Double tOld = avg(older, PerformanceSnapshot::tps1m);
        if (tNew != null && tOld != null && tOld > 0) {
            double pct = (tNew - tOld) / tOld * 100.0;
            if (pct <= -tpsDropPct) {
                parts.add("TPS " + f1(tOld) + " -> " + f1(tNew) + " (" + pct(pct) + ")");
                severity = Severity.max(severity, dropSeverity(-pct));
            }
        }

        // MSPT - higher is worse
        Double mNew = avg(newer, PerformanceSnapshot::mspt);
        Double mOld = avg(older, PerformanceSnapshot::mspt);
        if (mNew != null && mOld != null && mOld > 0) {
            double pct = (mNew - mOld) / mOld * 100.0;
            if (pct >= msptRisePct) {
                parts.add("MSPT " + f1(mOld) + "ms -> " + f1(mNew) + "ms (" + pct(pct) + ")");
                severity = Severity.max(severity, riseSeverity(pct, 50, 100));
            }
        }

        // RAM (used MB) - higher is worse
        Double rNew = avg(newer, s -> (double) s.memory().usedMb());
        Double rOld = avg(older, s -> (double) s.memory().usedMb());
        if (rNew != null && rOld != null && rOld > 0) {
            double pct = (rNew - rOld) / rOld * 100.0;
            if (pct >= ramRisePct) {
                parts.add("RAM " + f0(rOld) + "MB -> " + f0(rNew) + "MB (" + pct(pct) + ")");
                severity = Severity.max(severity, riseSeverity(pct, 60, 100));
            }
        }

        if (parts.isEmpty()) return out.build();

        out.finding(new Finding(id(), severity,
                "Performance regression vs baseline (" + older.size() + " older / "
                        + newer.size() + " newer samples): " + String.join("; ", parts)));
        return out.build();
    }

    private static Double avg(List<PerformanceSnapshot> list, ToDoubleFunction<PerformanceSnapshot> f) {
        double sum = 0;
        int n = 0;
        for (PerformanceSnapshot s : list) {
            double v = f.applyAsDouble(s);
            if (!Double.isNaN(v)) { sum += v; n++; }
        }
        return n == 0 ? null : sum / n;
    }

    private static Severity dropSeverity(double dropPct) {
        if (dropPct > 30) return Severity.CRITICAL;
        if (dropPct >= 15) return Severity.HIGH;
        return Severity.MEDIUM;
    }

    private static Severity riseSeverity(double risePct, double highAt, double critAt) {
        if (risePct > critAt) return Severity.CRITICAL;
        if (risePct >= highAt) return Severity.HIGH;
        return Severity.MEDIUM;
    }

    private static String pct(double v) {
        return (v >= 0 ? "+" : "") + Math.round(v) + "%";
    }
    private static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }
    private static String f0(double v) { return String.format(Locale.ROOT, "%.0f", v); }
}
