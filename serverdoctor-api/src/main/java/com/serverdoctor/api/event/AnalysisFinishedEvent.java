package com.serverdoctor.api.event;

import com.serverdoctor.api.module.DiagnosticReport;

import java.time.Instant;

public record AnalysisFinishedEvent(DiagnosticReport report, Instant timestamp)
        implements ServerDoctorEvent {

    public AnalysisFinishedEvent(DiagnosticReport report) { this(report, Instant.now()); }
}
