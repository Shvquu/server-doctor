package com.serverdoctor.bungee.platform;

import com.serverdoctor.platform.SchedulerAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;

/** A proxy has no region/entity context - everything runs on the proxy scheduler (async). */
public final class BungeeSchedulerAdapter implements SchedulerAdapter {

    private final ProxyServer proxy;
    private final Plugin plugin;

    public BungeeSchedulerAdapter(ProxyServer proxy, Plugin plugin) {
        this.proxy = proxy;
        this.plugin = plugin;
    }

    @Override public void runAsync(Runnable task) {
        proxy.getScheduler().runAsync(plugin, task);
    }

    @Override public void runGlobal(Runnable task) {
        proxy.getScheduler().runAsync(plugin, task);
    }

    @Override
    public Cancellable runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
        long delayMs = Math.max(1L, initialDelayTicks * 50L);
        long periodMs = Math.max(1L, periodTicks * 50L);
        ScheduledTask handle = proxy.getScheduler()
                .schedule(plugin, task, delayMs, periodMs, TimeUnit.MILLISECONDS);
        return () -> proxy.getScheduler().cancel(handle);
    }

    @Override public void shutdown() {
        proxy.getScheduler().cancel(plugin);
    }
}
