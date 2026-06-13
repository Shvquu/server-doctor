package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.Severity;

import java.util.Set;

/** Erfasst installierte Plugins und meldet fehlende Metadaten. */
public final class PluginScanner implements AnalysisModule {

    @Override public String id() { return "plugin"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());
        var plugins = context.plugins();
        out.finding(new Finding(id(), Severity.INFO, plugins.size() + " Plugin(s) erkannt."));
        for (PluginInfo p : plugins) {
            if (!p.hasVersion()) {
                out.finding(new Finding(id(), Severity.LOW,
                        "Plugin '" + p.name() + "' hat keine Versionsangabe."));
            }
        }
        return out.build();
    }
}
