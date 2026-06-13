package com.serverdoctor.velocity.platform;

import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.ServerInfo;
import com.serverdoctor.platform.CommandAdapter;
import com.serverdoctor.platform.LoggerAdapter;
import com.serverdoctor.platform.MetricsAdapter;
import com.serverdoctor.platform.PlayerAdapter;
import com.serverdoctor.platform.PluginAdapter;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.platform.ServerPlatform;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.util.Set;

/** Velocity-Implementierung der Plattform-Fassade (Proxy: kein Tick-Loop, keine Entities). */
public final class VelocityServerPlatform implements ServerPlatform {

    private final ProxyServer proxy;
    private final PluginAdapter pluginAdapter;
    private final PlayerAdapter playerAdapter;
    private final MetricsAdapter metricsAdapter = new VelocityMetricsAdapter();
    private final SchedulerAdapter schedulerAdapter;
    private final LoggerAdapter loggerAdapter;

    public VelocityServerPlatform(ProxyServer proxy, Logger logger, Object plugin) {
        this.proxy = proxy;
        this.pluginAdapter = new VelocityPluginAdapter(proxy);
        this.playerAdapter = new VelocityPlayerAdapter(proxy);
        this.schedulerAdapter = new VelocitySchedulerAdapter(proxy, plugin);
        this.loggerAdapter = new VelocityLoggerAdapter(logger);
    }

    @Override public String name() { return "Velocity"; }

    @Override
    public ServerInfo serverInfo() {
        return new ServerInfo("Velocity", proxy.getVersion().getVersion(),
                System.getProperty("java.version", "unknown"));
    }

    @Override
    public Set<Capability> capabilities() {
        return Set.of(Capability.HAS_PLUGINS, Capability.IS_PROXY);
    }

    @Override public PluginAdapter plugins() { return pluginAdapter; }
    @Override public PlayerAdapter players() { return playerAdapter; }
    @Override public MetricsAdapter metrics() { return metricsAdapter; }
    @Override public SchedulerAdapter scheduler() { return schedulerAdapter; }
    @Override public LoggerAdapter logger() { return loggerAdapter; }

    /** Befehle werden direkt im Plugin registriert; kein Adapter nötig. */
    @Override public CommandAdapter commands() { return (name, handler) -> { }; }
}
