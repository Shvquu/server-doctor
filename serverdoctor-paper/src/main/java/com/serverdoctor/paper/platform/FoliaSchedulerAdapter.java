package com.serverdoctor.paper.platform;

import com.serverdoctor.platform.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Scheduler für Folia (region-threaded). Diese Klasse wird nur instanziiert,
 * wenn Folia zur Laufzeit erkannt wurde - die Folia-Scheduler-Typen werden
 * also nie auf normalem Paper/Spigot benötigt.
 */
public final class FoliaSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;

    public FoliaSchedulerAdapter(Plugin plugin) { this.plugin = plugin; }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    @Override
    public void runGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
    }

    @Override
    public Cancellable runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
        long initMs = Math.max(1L, initialDelayTicks * 50L);
        long periodMs = Math.max(1L, periodTicks * 50L);
        var handle = Bukkit.getAsyncScheduler()
                .runAtFixedRate(plugin, t -> task.run(), initMs, periodMs, TimeUnit.MILLISECONDS);
        return handle::cancel;
    }

    @Override
    public void shutdown() {
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
    }
}
