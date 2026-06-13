package com.serverdoctor.common.model;

import java.time.Instant;

/**
 * Immutabler Performance-Snapshot. Wird auf einem plattformkonformen Thread
 * erzeugt und danach unveränderlich weitergereicht (Folia-safe).
 */
public record PerformanceSnapshot(double[] tps, double mspt, MemoryStats memory,
                                  int threadCount, int onlinePlayers, Instant capturedAt) {

    public double tps1m() { return tps != null && tps.length > 0 ? tps[0] : Double.NaN; }

    public static PerformanceSnapshot empty() {
        return new PerformanceSnapshot(new double[0], Double.NaN,
                new MemoryStats(0, 0, 0, 0, 0), 0, 0, Instant.now());
    }
}
