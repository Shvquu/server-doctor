package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Severity;

import java.util.Set;

/** Bewertet TPS, MSPT und Speicherauslastung gegen Schwellenwerte. */
public final class PerformanceScanner implements AnalysisModule {

    @Override public String id() { return "performance"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_TICK_LOOP); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());
        PerformanceSnapshot s = context.performance();

        double tps = s.tps1m();
        if (!Double.isNaN(tps)) {
            if (tps < 10.0) {
                out.finding(new Finding(id(), Severity.CRITICAL,
                        String.format("Kritisch niedrige TPS: %.1f", tps)));
            } else if (tps < 15.0) {
                out.finding(new Finding(id(), Severity.HIGH,
                        String.format("Niedrige TPS: %.1f", tps)));
            } else if (tps < 18.0) {
                out.finding(new Finding(id(), Severity.MEDIUM,
                        String.format("Leicht reduzierte TPS: %.1f", tps)));
            }
        }

        if (!Double.isNaN(s.mspt()) && s.mspt() > 50.0) {
            out.finding(new Finding(id(), Severity.MEDIUM,
                    String.format("MSPT über 50ms: %.1fms", s.mspt())));
        }

        double mem = s.memory().usedRatio();
        if (!Double.isNaN(mem)) {
            if (mem > 0.95) {
                out.finding(new Finding(id(), Severity.HIGH,
                        String.format("Speicher fast voll: %.0f%% (%d/%d MB)",
                                mem * 100, s.memory().usedMb(), s.memory().maxMb())));
            } else if (mem > 0.85) {
                out.finding(new Finding(id(), Severity.MEDIUM,
                        String.format("Hohe Speicherauslastung: %.0f%%", mem * 100)));
            }
        }
        return out.build();
    }
}
