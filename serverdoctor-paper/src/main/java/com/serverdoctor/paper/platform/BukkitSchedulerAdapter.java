package com.serverdoctor.paper.platform;

import com.serverdoctor.platform.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** Paper/Spigot-Scheduler. Auf Folia wird dieser Adapter ersetzt. */
public final class BukkitSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;

    public BukkitSchedulerAdapter(Plugin plugin) { this.plugin = plugin; }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runGlobal(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public Cancellable runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
        BukkitTask handle = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(plugin, task, initialDelayTicks, periodTicks);
        return handle::cancel;
    }

    @Override
    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
