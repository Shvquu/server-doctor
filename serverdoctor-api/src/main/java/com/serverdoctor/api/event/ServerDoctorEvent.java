package com.serverdoctor.api.event;

import java.time.Instant;

/** Basis aller ServerDoctor-Events. */
public interface ServerDoctorEvent {
    Instant timestamp();
}
