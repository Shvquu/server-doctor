package com.serverdoctor.core.baseline;

import com.serverdoctor.common.model.Severity;

import java.time.Instant;
import java.util.Map;

/** A pinned "known-good" snapshot to diff later runs against. */
public record Baseline(Instant pinnedAt, String serverVersion,
                       double tps1m, double mspt, long memUsedMb,
                       int conflicts, int securityRisks, int recommendations,
                       Map<Severity, Integer> findingsBySeverity) {
    public Baseline {
        findingsBySeverity = findingsBySeverity == null ? Map.of() : Map.copyOf(findingsBySeverity);
    }
    public int totalFindings() { return findingsBySeverity.values().stream().mapToInt(Integer::intValue).sum(); }
}
