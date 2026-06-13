package com.serverdoctor.core.engine;

import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.ServerInfo;
import com.serverdoctor.platform.ServerPlatform;

import java.util.List;
import java.util.Set;

/**
 * Snapshot-basierte Implementierung der api-Facade. Alle Werte werden bei der
 * Konstruktion einmalig erfasst und sind danach immutable (thread-safe).
 */
public final class CoreServerContext implements ServerContext {

    private final Set<Capability> capabilities;
    private final List<PluginInfo> plugins;
    private final PerformanceSnapshot performance;
    private final ServerInfo serverInfo;

    public CoreServerContext(ServerPlatform platform, PerformanceSnapshot performance) {
        this.capabilities = Set.copyOf(platform.capabilities());
        this.plugins = List.copyOf(platform.plugins().installed());
        this.serverInfo = platform.serverInfo();
        this.performance = performance;
    }

    @Override public Set<Capability> capabilities() { return capabilities; }
    @Override public boolean has(Capability capability) { return capabilities.contains(capability); }
    @Override public List<PluginInfo> plugins() { return plugins; }
    @Override public PerformanceSnapshot performance() { return performance; }
    @Override public ServerInfo serverInfo() { return serverInfo; }
}
