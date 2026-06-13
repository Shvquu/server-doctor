package com.serverdoctor.testing;

import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.ServerInfo;
import com.serverdoctor.platform.CommandAdapter;
import com.serverdoctor.platform.LoggerAdapter;
import com.serverdoctor.platform.MetricsAdapter;
import com.serverdoctor.platform.PlayerAdapter;
import com.serverdoctor.platform.PluginAdapter;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.platform.ServerPlatform;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Konfigurierbare Test-Plattform. Scheduler läuft inline, Logger no-op.
 * Über den Builder lassen sich Plugins, Capabilities und Metriken setzen.
 */
public final class FakeServerPlatform implements ServerPlatform {

    private final Set<Capability> capabilities;
    private final List<PluginInfo> plugins;
    private final double[] tps;
    private final double mspt;
    private final MemoryStats memory;
    private final int threads;
    private final int players;
    private final int maxPlayers;

    private FakeServerPlatform(Builder b) {
        this.capabilities = Set.copyOf(b.capabilities);
        this.plugins = List.copyOf(b.plugins);
        this.tps = b.tps;
        this.mspt = b.mspt;
        this.memory = b.memory;
        this.threads = b.threads;
        this.players = b.players;
        this.maxPlayers = b.maxPlayers;
    }

    public static Builder builder() { return new Builder(); }

    @Override public String name() { return "FakePlatform"; }
    @Override public ServerInfo serverInfo() { return new ServerInfo("Fake", "test", "21"); }
    @Override public Set<Capability> capabilities() { return capabilities; }

    @Override public PluginAdapter plugins() { return () -> plugins; }

    @Override public PlayerAdapter players() {
        return new PlayerAdapter() {
            @Override public int onlineCount() { return players; }
            @Override public int maxPlayers() { return maxPlayers; }
        };
    }

    @Override public MetricsAdapter metrics() {
        return new MetricsAdapter() {
            @Override public double[] tps() { return tps; }
            @Override public double mspt() { return mspt; }
            @Override public MemoryStats memory() { return memory; }
            @Override public int threadCount() { return threads; }
        };
    }

    @Override public SchedulerAdapter scheduler() {
        return new SchedulerAdapter() {
            @Override public void runAsync(Runnable t) { t.run(); }
            @Override public void runGlobal(Runnable t) { t.run(); }
            @Override public Cancellable runRepeatingAsync(Runnable t, long i, long p) { return () -> {}; }
            @Override public void shutdown() {}
        };
    }

    @Override public LoggerAdapter logger() {
        return new LoggerAdapter() {
            @Override public void info(String m) {}
            @Override public void warn(String m) {}
            @Override public void error(String m, Throwable t) {}
        };
    }

    @Override public CommandAdapter commands() { return (n, h) -> {}; }

    public static final class Builder {
        private Set<Capability> capabilities = EnumSet.of(
                Capability.HAS_PLUGINS, Capability.HAS_TICK_LOOP, Capability.HAS_ENTITIES);
        private final List<PluginInfo> plugins = new ArrayList<>();
        private double[] tps = {20.0, 20.0, 20.0};
        private double mspt = 5.0;
        private MemoryStats memory = new MemoryStats(1024L*1024*1024, 2048L*1024*1024, 4096L*1024*1024, 1, 1);
        private int threads = 30;
        private int players = 0;
        private int maxPlayers = 100;

        public Builder capabilities(Capability... caps) {
            this.capabilities = caps.length == 0 ? EnumSet.noneOf(Capability.class) : EnumSet.copyOf(List.of(caps));
            return this;
        }
        public Builder plugin(PluginInfo p) { this.plugins.add(p); return this; }
        public Builder plugin(String name, String version) {
            return plugin(new PluginInfo(name, version, List.of("Author"), List.of(), List.of(), true));
        }
        public Builder tps(double oneMin) { this.tps = new double[]{oneMin, oneMin, oneMin}; return this; }
        public Builder mspt(double v) { this.mspt = v; return this; }
        public Builder memory(MemoryStats m) { this.memory = m; return this; }
        public Builder memoryRatio(double usedRatio) {
            long max = 4096L*1024*1024;
            this.memory = new MemoryStats((long)(max*usedRatio), max, max, 1, 1);
            return this;
        }
        public Builder players(int n) { this.players = n; return this; }

        public FakeServerPlatform build() { return new FakeServerPlatform(this); }
    }
}
