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

import java.util.EnumSet;
import java.util.Set;

/** Bukkit-Implementierung der Plattform-Fassade - unterstützt Paper und Folia. */
public final class PaperServerPlatform implements ServerPlatform {

    private final boolean folia = detectFolia();

    private final PluginAdapter pluginAdapter = new PaperPluginAdapter();
    private final PlayerAdapter playerAdapter = new PaperPlayerAdapter();
    private final MetricsAdapter metricsAdapter = new PaperMetricsAdapter();
    private final SchedulerAdapter schedulerAdapter;
    private final LoggerAdapter loggerAdapter;
    private final CommandAdapter commandAdapter;

    public PaperServerPlatform(JavaPlugin plugin) {
        this.schedulerAdapter = folia
                ? new FoliaSchedulerAdapter(plugin)
                : new BukkitSchedulerAdapter(plugin);
        this.loggerAdapter = new PaperLoggerAdapter(plugin.getLogger());
        this.commandAdapter = new PaperCommandAdapter(plugin);
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override public String name() { return folia ? "Folia" : "Paper"; }

    @Override
    public ServerInfo serverInfo() {
        return new ServerInfo(name(), Bukkit.getVersion(),
                System.getProperty("java.version", "unknown"));
    }

    @Override
    public Set<Capability> capabilities() {
        EnumSet<Capability> caps = EnumSet.of(
                Capability.HAS_PLUGINS, Capability.HAS_TICK_LOOP, Capability.HAS_ENTITIES);
        if (folia) caps.add(Capability.HAS_REGIONS);
        return caps;
    }

    @Override public PluginAdapter plugins() { return pluginAdapter; }
    @Override public PlayerAdapter players() { return playerAdapter; }
    @Override public MetricsAdapter metrics() { return metricsAdapter; }
    @Override public SchedulerAdapter scheduler() { return schedulerAdapter; }
    @Override public LoggerAdapter logger() { return loggerAdapter; }
    @Override public CommandAdapter commands() { return commandAdapter; }
}
