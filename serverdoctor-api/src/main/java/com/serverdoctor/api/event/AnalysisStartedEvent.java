package com.serverdoctor.api.event;

import java.time.Instant;

/** Fired right before a diagnostics run begins. */
public record AnalysisStartedEvent(Instant timestamp) implements ServerDoctorEvent {
    public AnalysisStartedEvent() { this(Instant.now()); }
}
