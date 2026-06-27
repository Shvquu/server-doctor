package com.serverdoctor.api.event;

import com.serverdoctor.common.model.Severity;

import java.time.Instant;

/** Fired when the overall severity differs from the previous run's overall severity. */
public record OverallSeverityChangedEvent(Severity previous, Severity current, Instant timestamp)
        implements ServerDoctorEvent {

    public OverallSeverityChangedEvent(Severity previous, Severity current) {
        this(previous, current, Instant.now());
    }

    /** True if the status got worse (current is more severe than previous). */
    public boolean worsened() { return current.atLeast(previous) && current != previous; }

    /** True if the status improved (current is less severe than previous). */
    public boolean improved() { return previous.atLeast(current) && current != previous; }
}
