package com.serverdoctor.platform;

/**
 * Abstraktion über plattformabhängiges Scheduling.
 * Der Core trifft keine Annahme über einen globalen Main-Thread (Folia-safe).
 */
public interface SchedulerAdapter {

    void runAsync(Runnable task);

    void runGlobal(Runnable task);

    /** Wiederholend, asynchron. Angaben in Ticks (1 Tick = 50 ms). */
    Cancellable runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks);

    void shutdown();

    interface Cancellable { void cancel(); }
}
