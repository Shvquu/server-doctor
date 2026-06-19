package com.serverdoctor.core.regression;

import com.serverdoctor.common.model.PerformanceSnapshot;

import java.util.List;

/**
 * Read-only window into previously stored performance snapshots. Backed by the storage layer in
 * each platform adapter (e.g. {@code limit -> storage.performance().recent(limit)}). The default
 * is a no-op, so regression detection simply stays quiet until history is wired in.
 */
@FunctionalInterface
public interface PerformanceHistory {
    List<PerformanceSnapshot> recent(int limit);
}
