package com.serverdoctor.core.regression;

import com.serverdoctor.common.model.PerformanceSnapshot;
import java.util.List;

public final class NoopPerformanceHistory implements PerformanceHistory {

    public static final NoopPerformanceHistory INSTANCE = new NoopPerformanceHistory();

    private NoopPerformanceHistory() {}

    @Override
    public List<PerformanceSnapshot> recent(int limit) {
        return List.of();
    }
}
