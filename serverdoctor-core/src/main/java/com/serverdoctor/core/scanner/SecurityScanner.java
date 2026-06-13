package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;

import java.util.Set;

/**
 * Konservative, ehrliche Heuristiken zur Wartungs-/Sicherheitslage.
 * Hier wird KEINE CVE-Datenbank erfunden - das ist ein dedizierter Anschlusspunkt
 * für eine spätere AdvisorySource (Modrinth/GitHub Security Advisories).
 */
public final class SecurityScanner implements AnalysisModule {

    @Override public String id() { return "security"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());
        for (PluginInfo p : context.plugins()) {
            if (!p.hasVersion() || !p.hasAuthors()) {
                out.risk(new SecurityRisk(p.name(), SecurityRisk.RiskType.MISSING_METADATA,
                        Severity.LOW,
                        "Unvollständige Metadaten (Version/Autor) - Herkunft und Aktualität schwer prüfbar."));
            }
        }
        return out.build();
    }
}
