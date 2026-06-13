package com.serverdoctor.core.engine;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScannerRegistryTest {

    private AnalysisModule module(String id, Capability... required) {
        return new AnalysisModule() {
            @Override public String id() { return id; }
            @Override public Set<Capability> requiredCapabilities() { return Set.of(required); }
            @Override public AnalysisResult analyze(ServerContext c) { return AnalysisResult.empty(id); }
        };
    }

    @Test void filtersByAvailableCapabilities() {
        ScannerRegistry registry = new ScannerRegistry();
        registry.register(module("anywhere"));
        registry.register(module("ticking", Capability.HAS_TICK_LOOP));

        var proxyOnly = registry.applicableFor(Set.of(Capability.HAS_PLUGINS, Capability.IS_PROXY));
        assertEquals(1, proxyOnly.size(), "Tick-Scanner darf auf Proxy nicht laufen");
        assertEquals("anywhere", proxyOnly.get(0).id());

        var fullServer = registry.applicableFor(Set.of(Capability.HAS_PLUGINS, Capability.HAS_TICK_LOOP));
        assertEquals(2, fullServer.size());
    }

    @Test void unregisterRemovesModule() {
        ScannerRegistry registry = new ScannerRegistry();
        registry.register(module("x"));
        registry.unregister("x");
        assertTrue(registry.all().isEmpty());
    }

    @Test void registerSameIdReplaces() {
        ScannerRegistry registry = new ScannerRegistry();
        registry.register(module("dup"));
        registry.register(module("dup"));
        assertEquals(1, registry.all().size());
    }
}
