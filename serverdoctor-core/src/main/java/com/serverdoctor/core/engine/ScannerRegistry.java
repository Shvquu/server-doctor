package com.serverdoctor.core.engine;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.common.model.Capability;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe Registrierung aller Analyse-Module. */
public final class ScannerRegistry {

    private final Map<String, AnalysisModule> modules = new ConcurrentHashMap<>();

    public void register(AnalysisModule module) {
        modules.put(module.id(), module);
    }

    public void unregister(String id) { modules.remove(id); }

    /** Module, deren benötigte Capabilities von der Plattform erfüllt werden. */
    public List<AnalysisModule> applicableFor(Set<Capability> available) {
        return modules.values().stream()
                .filter(m -> available.containsAll(m.requiredCapabilities()))
                .toList();
    }

    public List<AnalysisModule> all() { return List.copyOf(modules.values()); }
}
