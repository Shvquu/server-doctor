package com.serverdoctor.core.baseline;

import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.PerformanceSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Diffs a current report against a pinned {@link Baseline} and produces human-readable lines. */
public final class BaselineComparator {

    public List<String> compare(Baseline base, DiagnosticReport now) {
        List<String> out = new ArrayList<>();
        PerformanceSnapshot p = now.performance();

        out.add("Baseline pinned " + base.pinnedAt()
        + (base.serverVersion().isBlank() ? "" : " (server " + base.serverVersion() + ")"));


        if (p != null) {
            if (!Double.isNaN(base.tps1m()) && !Double.isNaN(p.tps1m()))
                out.add(deltaLine("TPS (1m)", base.tps1m(), p.tps1m(), 2, true));
            if (!Double.isNaN(base.mspt()) && !Double.isNaN(p.mspt()))
                out.add(deltaLine("MSPT", base.mspt(), p.mspt(), 2, false));
            out.add(deltaLine("RAM used (MB)", base.memUsedMb(), p.memory().usedMb(), 0, false));
        }
        out.add(countLine("Conflicts", base.conflicts(), now.conflicts().size()));
        out.add(countLine("Security risks", base.securityRisks(), now.securityRisks().size()));
        out.add(countLine("Recommendations", base.recommendations(), now.recommendations().size()));

        int nowFindings = 0;
        for (var res : now.results()) nowFindings += res.findings().size();
        out.add(countLine("Findings", base.totalFindings(), nowFindings));
        return out;
    }

    private static String deltaLine(String label, double base, double now, int decimals, boolean higherIsBetter) {
        double diff = now - base;
        boolean worse = higherIsBetter ? diff < 0 : diff > 0;
        boolean better = higherIsBetter ? diff > 0 : diff < 0;
        String arrow = Math.abs(diff) < 1e-9 ? "=" : (worse ? "▼ worse" : (better ? "▲ better" : ""));
        String f = "%." + decimals + "f";
        return String.format(Locale.ROOT, "%s: " + f + " → " + f + " (" + (diff >= 0 ? "+" : "") + f + ") %s",
                label, base, now, diff, arrow).trim();
    }

    private static String countLine(String label, int base, int now) {
        int diff = now - base;
        String tag = diff > 0 ? " ▼ +" + diff + " new" : (diff < 0 ? " ▲ " + diff : " =");
        return label + ": " + base + " → " + now + tag;
    }

}
