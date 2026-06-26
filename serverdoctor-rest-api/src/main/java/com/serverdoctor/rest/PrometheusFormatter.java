package com.serverdoctor.rest;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Severity;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Renders the latest metrics in the Prometheus text exposition format (v0.0.4). Read-only. */
final class PrometheusFormatter {

    private PrometheusFormatter() {}

    static String render(PerformanceSnapshot p, Optional<DiagnosticReport> report) {
        StringBuilder b = new StringBuilder();

        b.append("# HELP serverdoctor_up 1 if ServerDoctor is active.\n");
        b.append("# TYPE serverdoctor_up gauge\nserverdoctor_up 1\n");

        if (p != null) {
            type(b, "serverdoctor_tps", "gauge", "Ticks per second.");
            tps(b, "1m", p.tps1m());
            tps(b, "5m", at(p, 1));
            tps(b, "15m", at(p, 2));

            gauge(b, "serverdoctor_mspt", "Milliseconds per tick.", p.mspt());
            gauge(b, "serverdoctor_memory_used_bytes", "Heap used (bytes).", p.memory().usedBytes());
            gauge(b, "serverdoctor_memory_max_bytes", "Heap max (bytes).", p.memory().maxBytes());
            gauge(b, "serverdoctor_memory_used_ratio", "Heap used ratio (0..1).", p.memory().usedRatio());
            gauge(b, "serverdoctor_players_online", "Players online.", p.onlinePlayers());
            gauge(b, "serverdoctor_threads", "Live thread count.", p.threadCount());
        }

        if (report.isPresent()) {
            DiagnosticReport r = report.get();
            gauge(b, "serverdoctor_overall_severity",
                    "Overall severity (0=OK .. 5=CRITICAL).", r.overallSeverity().ordinal());

            Map<Severity, Integer> bySev = new EnumMap<>(Severity.class);
            for (AnalysisResult res : r.results())
                for (Finding f : res.findings())
                    bySev.merge(f.severity(), 1, Integer::sum);
            type(b, "serverdoctor_findings", "gauge", "Findings by severity.");
            for (Severity s : Severity.values())
                b.append("serverdoctor_findings{severity=\"").append(s.name()).append("\"} ")
                        .append(bySev.getOrDefault(s, 0)).append('\n');

            gauge(b, "serverdoctor_conflicts_total", "Detected conflicts.", r.conflicts().size());
            gauge(b, "serverdoctor_security_risks_total", "Security risks.", r.securityRisks().size());
            gauge(b, "serverdoctor_recommendations_total", "Recommendations.", r.recommendations().size());
        }
        return b.toString();
    }

    private static double at(PerformanceSnapshot p, int i) {
        double[] t = p.tps();
        return t != null && t.length > i ? t[i] : Double.NaN;
    }

    private static void tps(StringBuilder b, String window, double v) {
        if (!Double.isNaN(v)) b.append("serverdoctor_tps{window=\"").append(window).append("\"} ").append(num(v)).append('\n');
    }

    private static void type(StringBuilder b, String name, String type, String help) {
        b.append("# HELP ").append(name).append(' ').append(help).append('\n');
        b.append("# TYPE ").append(name).append(' ').append(type).append('\n');
    }

    private static void gauge(StringBuilder b, String name, String help, double value) {
        if (Double.isNaN(value)) return;
        type(b, name, "gauge", help);
        b.append(name).append(' ').append(num(value)).append('\n');
    }

    private static String num(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.format(Locale.ROOT, "%.4f", v);
    }
}
