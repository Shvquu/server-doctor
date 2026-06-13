package com.serverdoctor.velocity.platform;

import com.serverdoctor.platform.SchedulerAdapter;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;

import java.time.Duration;

/** Auf einem Proxy gibt es keinen Region-/Entity-Kontext - alles läuft über den Proxy-Scheduler. */
public final class VelocitySchedulerAdapter implements SchedulerAdapter {

    private final ProxyServer proxy;
    private final Object plugin;

    public VelocitySchedulerAdapter(ProxyServer proxy, Object plugin) {
        this.proxy = proxy;
        this.plugin = plugin;
    }

    @Override public void runAsync(Runnable task) {
        proxy.getScheduler().buildTask(plugin, task).schedule();
    }

    @Override public void runGlobal(Runnable task) {
        proxy.getScheduler().buildTask(plugin, task).schedule();
    }

    @Override
    public Cancellable runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
        ScheduledTask handle = proxy.getScheduler().buildTask(plugin, task)
                .delay(Duration.ofMillis(Math.max(1L, initialDelayTicks * 50L)))
                .repeat(Duration.ofMillis(Math.max(1L, periodTicks * 50L)))
                .schedule();
        return handle::cancel;
    }

    @Override public void shutdown() { /* Velocity bricht Tasks beim Plugin-Stop selbst ab */ }
}
