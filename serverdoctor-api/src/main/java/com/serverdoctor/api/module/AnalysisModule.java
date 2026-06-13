package com.serverdoctor.api.module;

import com.serverdoctor.common.model.Capability;

import java.util.Set;

/**
 * Erweiterungspunkt: Fremd-Plugins implementieren dieses Interface und
 * registrieren es via {@code ServerDoctorApi.registerModule(...)}.
 */
public interface AnalysisModule {

    String id();

    /** Plattform-Anforderungen. Standard: keine - läuft überall. */
    default Set<Capability> requiredCapabilities() { return Set.of(); }

    AnalysisResult analyze(ServerContext context);
}
