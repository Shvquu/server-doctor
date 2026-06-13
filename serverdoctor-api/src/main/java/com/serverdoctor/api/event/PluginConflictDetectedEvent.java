package com.serverdoctor.api.event;

import com.serverdoctor.common.model.ConflictReport;

import java.time.Instant;

public record PluginConflictDetectedEvent(ConflictReport conflict, Instant timestamp)
        implements ServerDoctorEvent {

    public PluginConflictDetectedEvent(ConflictReport conflict) { this(conflict, Instant.now()); }
}
