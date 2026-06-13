package com.serverdoctor.core.recommendation;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;

import java.util.ArrayList;
import java.util.List;

/** Leitet aus Befunden konkrete Empfehlungen ab. Reine Daten, keine Aktionen. */
public final class RecommendationEngine {

    public List<Recommendation> evaluate(List<AnalysisResult> results) {
        List<Recommendation> out = new ArrayList<>();
        int counter = 0;

        for (AnalysisResult result : results) {
            for (ConflictReport c : result.conflicts()) {
                out.add(new Recommendation("rec-" + (counter++), Recommendation.Category.CONFLICT,
                        c.severity(),
                        "Konflikt prüfen: " + c.pluginA() + " + " + c.pluginB(),
                        c.description() + " Empfehlung: Funktionsüberschneidung prüfen und eines der Plugins entfernen oder umkonfigurieren."));
            }
            for (SecurityRisk r : result.securityRisks()) {
                out.add(new Recommendation("rec-" + (counter++), Recommendation.Category.SECURITY,
                        r.severity(),
                        "Sicherheit/Wartung: " + r.pluginName(),
                        r.description()));
            }
            for (Finding f : result.findings()) {
                if ("performance".equals(f.scannerId()) && f.severity().atLeast(Severity.MEDIUM)) {
                    out.add(new Recommendation("rec-" + (counter++), Recommendation.Category.PERFORMANCE,
                            f.severity(),
                            "Performance untersuchen",
                            f.message() + " Empfehlung: aufwendige Plugins/Chunk-Last/Entity-Count prüfen."));
                }
            }
        }
        return out;
    }
}
