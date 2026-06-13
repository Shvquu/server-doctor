package com.serverdoctor.api.event;

import com.serverdoctor.common.model.SecurityRisk;

import java.time.Instant;

public record SecurityRiskDetectedEvent(SecurityRisk risk, Instant timestamp)
        implements ServerDoctorEvent {

    public SecurityRiskDetectedEvent(SecurityRisk risk) { this(risk, Instant.now()); }
}
