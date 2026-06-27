package com.serverdoctor.api.event;

import com.serverdoctor.common.model.Recommendation;

import java.time.Instant;

/** Fired once per recommendation produced by a run (symmetric to conflict/risk events). */
public record RecommendationGeneratedEvent(Recommendation recommendation, Instant timestamp)
        implements ServerDoctorEvent {
    public RecommendationGeneratedEvent(Recommendation recommendation) {
        this(recommendation, Instant.now());
    }
}
