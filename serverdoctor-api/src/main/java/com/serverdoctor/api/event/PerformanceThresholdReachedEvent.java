package com.serverdoctor.api.event;

import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Severity;

import java.time.Instant;

public record PerformanceThresholdReachedEvent(PerformanceSnapshot snapshot, Severity severity,
                                               String reason, Instant timestamp)
        implements ServerDoctorEvent {

    public PerformanceThresholdReachedEvent(PerformanceSnapshot s, Severity sev, String reason) {
        this(s, sev, reason, Instant.now());
    }
}
