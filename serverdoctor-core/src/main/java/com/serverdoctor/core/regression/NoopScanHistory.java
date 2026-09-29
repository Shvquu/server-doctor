package com.serverdoctor.core.regression;

import com.serverdoctor.common.model.ScanSummary;
import java.util.List;

public final class NoopScanHistory implements ScanHistory {

    public static final NoopScanHistory INSTANCE = new NoopScanHistory();

    private NoopScanHistory() {}

    @Override
    public List<ScanSummary> recent(int limit) {
        return List.of();
    }
}
