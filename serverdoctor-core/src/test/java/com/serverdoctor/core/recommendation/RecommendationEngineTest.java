package com.serverdoctor.core.recommendation;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationEngineTest {

    private final RecommendationEngine engine = new RecommendationEngine();

    @Test void createsRecommendationForConflict() {
        var result = AnalysisResult.builder("conflict")
                .conflict(new ConflictReport("c", "A", "B", Severity.HIGH, "desc"))
                .build();
        List<Recommendation> recs = engine.evaluate(List.of(result));
        assertEquals(1, recs.size());
        assertEquals(Recommendation.Category.CONFLICT, recs.get(0).category());
    }

    @Test void createsRecommendationForSignificantPerformanceFinding() {
        var result = AnalysisResult.builder("performance")
                .finding(new Finding("performance", Severity.HIGH, "Niedrige TPS"))
                .build();
        List<Recommendation> recs = engine.evaluate(List.of(result));
        assertEquals(1, recs.size());
        assertEquals(Recommendation.Category.PERFORMANCE, recs.get(0).category());
    }

    @Test void ignoresMinorInfoFindings() {
        var result = AnalysisResult.builder("plugin")
                .finding(new Finding("plugin", Severity.INFO, "3 Plugins"))
                .build();
        assertTrue(engine.evaluate(List.of(result)).isEmpty());
    }
}
