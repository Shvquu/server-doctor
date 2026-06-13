package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.core.conflict.ConflictDatabase;
import com.serverdoctor.core.conflict.ConflictDefinition;

import java.util.HashSet;
import java.util.Set;

/** Gleicht installierte Plugins gegen die Konfliktdatenbank ab. */
public final class ConflictScanner implements AnalysisModule {

    private final ConflictDatabase database;

    public ConflictScanner(ConflictDatabase database) { this.database = database; }

    @Override public String id() { return "conflict"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());
        Set<String> names = new HashSet<>();
        context.plugins().forEach(p -> names.add(p.name().toLowerCase()));

        for (ConflictDefinition def : database.all()) {
            if (names.contains(def.pluginA().toLowerCase()) && names.contains(def.pluginB().toLowerCase())) {
                out.conflict(new ConflictReport(def.id(), def.pluginA(), def.pluginB(),
                        def.severity(), def.description()));
            }
        }
        return out.build();
    }
}
