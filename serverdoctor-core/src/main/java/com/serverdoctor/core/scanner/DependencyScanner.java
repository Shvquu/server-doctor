package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.Severity;

import java.util.HashSet;
import java.util.Set;

/** Prüft, ob harte Abhängigkeiten installiert sind. */
public final class DependencyScanner implements AnalysisModule {

    @Override public String id() { return "dependency"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());
        Set<String> present = new HashSet<>();
        context.plugins().forEach(p -> present.add(p.name().toLowerCase()));

        for (PluginInfo p : context.plugins()) {
            for (String dep : p.hardDepends()) {
                if (!present.contains(dep.toLowerCase())) {
                    out.finding(new Finding(id(), Severity.HIGH,
                            "Plugin '" + p.name() + "' benötigt fehlende Abhängigkeit '" + dep + "'."));
                }
            }
        }
        return out.build();
    }
}
