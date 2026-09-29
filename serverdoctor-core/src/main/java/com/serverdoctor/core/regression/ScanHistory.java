package com.serverdoctor.core.regression;

import com.serverdoctor.common.model.ScanSummary;

import java.util.List;

/**
 * Read-only window into previously stored per-scan counters (findings, conflicts, security
 * risks). Backed by the storage layer in each platform adapter
 * (e.g. {@code limit -> storage.summaries().recent(limit)}). The default is a no-op, so
 * count-trend detection simply stays quiet until history is wired in.
 */
@FunctionalInterface
public interface ScanHistory {
    List<ScanSummary> recent(int limit);
}
