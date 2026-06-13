package com.serverdoctor.storage.repository;

import com.serverdoctor.common.model.Recommendation;

import java.time.Instant;
import java.util.List;

public interface RecommendationRepository {
    void save(Instant at, Recommendation recommendation);
    List<Recommendation> recent(int limit);
}
