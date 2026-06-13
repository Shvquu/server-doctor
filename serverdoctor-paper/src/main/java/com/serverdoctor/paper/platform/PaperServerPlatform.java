package com.serverdoctor.paper.platform;

import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.ServerInfo;
import com.serverdoctor.platform.CommandAdapter;
import com.serverdoctor.platform.LoggerAdapter;
import com.serverdoctor.platform.MetricsAdapter;
import com.serverdoctor.platform.PlayerAdapter;
import com.serverdoctor.platform.PluginAdapter;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.platform.ServerPlatform;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

/** Paper-Implementierung der Plattform-Fassade. */
public final class PaperServerPlatform implements ServerPlatform {

    private final PluginAdapter pluginAdapter = new PaperPluginAdapter();
    private final PlayerAdapter playerAdapter = new PaperPlayerAdapter();
    private final MetricsAdapter metricsAdapter = new PaperMetricsAdapter();
    private final SchedulerAdapter schedulerAdapter;
    private final LoggerAdapter loggerAdapter;
    private final CommandAdapter commandAdapter;

    public PaperServerPlatform(JavaPlugin plugin) {
        this.schedulerAdapter = new BukkitSchedulerAdapter(plugin);
        this.loggerAdapter = new PaperLoggerAdapter(plugin.getLogger());
        this.commandAdapter = new PaperCommandAdapter(plugin);
    }

    @Override public String name() { return "Paper"; }

    @Override
    public ServerInfo serverInfo() {
        return new ServerInfo(Bukkit.getName(), Bukkit.getVersion(),
                System.getProperty("java.version", "unknown"));
    }

    @Override
    public Set<Capability> capabilities() {
        return Set.of(Capability.HAS_PLUGINS, Capability.HAS_TICK_LOOP, Capability.HAS_ENTITIES);
    }

    @Override public PluginAdapter plugins() { return pluginAdapter; }
    @Override public PlayerAdapter players() { return playerAdapter; }
    @Override public MetricsAdapter metrics() { return metricsAdapter; }
    @Override public SchedulerAdapter scheduler() { return schedulerAdapter; }
    @Override public LoggerAdapter logger() { return loggerAdapter; }
    @Override public CommandAdapter commands() { return commandAdapter; }
}
