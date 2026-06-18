package com.serverdoctor.bungee.platform;

import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.ServerInfo;
import com.serverdoctor.platform.CommandAdapter;
import com.serverdoctor.platform.LoggerAdapter;
import com.serverdoctor.platform.MetricsAdapter;
import com.serverdoctor.platform.PlayerAdapter;
import com.serverdoctor.platform.PluginAdapter;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.platform.ServerPlatform;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Set;
import java.util.logging.Logger;

/** BungeeCord implementation of the platform facade (proxy: no tick loop, no entities). */
public final class BungeeServerPlatform implements ServerPlatform {

    private final ProxyServer proxy;
    private final PluginAdapter pluginAdapter;
    private final PlayerAdapter playerAdapter;
    private final MetricsAdapter metricsAdapter = new BungeeMetricsAdapter();
    private final SchedulerAdapter schedulerAdapter;
    private final LoggerAdapter loggerAdapter;

    public BungeeServerPlatform(ProxyServer proxy, Logger logger, Plugin plugin) {
        this.proxy = proxy;
        this.pluginAdapter = new BungeePluginAdapter(proxy);
        this.playerAdapter = new BungeePlayerAdapter(proxy);
        this.schedulerAdapter = new BungeeSchedulerAdapter(proxy, plugin);
        this.loggerAdapter = new BungeeLoggerAdapter(logger);
    }

    @Override public String name() { return "BungeeCord"; }

    @Override
    public ServerInfo serverInfo() {
        return new ServerInfo("BungeeCord", proxy.getVersion(),
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

    /** Commands are registered directly in the plugin; no adapter needed. */
    @Override public CommandAdapter commands() { return (name, handler) -> { }; }
}
