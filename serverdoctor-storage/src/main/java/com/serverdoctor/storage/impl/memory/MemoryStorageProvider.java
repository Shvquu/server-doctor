package com.serverdoctor.storage.impl.memory;

import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.repository.ConflictRepository;
import com.serverdoctor.storage.repository.PerformanceRepository;
import com.serverdoctor.storage.repository.PluginRepository;
import com.serverdoctor.storage.repository.RecommendationRepository;
import com.serverdoctor.storage.repository.SecurityRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread-safe In-Memory-Implementierung. Default ohne konfigurierte DB und
 * praktisch für Tests. Hält pro Repository ein begrenztes Ringgedächtnis.
 */
public final class MemoryStorageProvider implements StorageProvider {

    private static final int MAX_ENTRIES = 10_000;

    private final Deque<PerformanceSnapshot> perf = new ConcurrentLinkedDeque<>();
    private final Deque<ConflictReport> conflictList = new ConcurrentLinkedDeque<>();
    private final Deque<SecurityRisk> riskList = new ConcurrentLinkedDeque<>();
    private final Deque<Recommendation> recList = new ConcurrentLinkedDeque<>();
    private volatile List<PluginInfo> inventory = List.of();

    @Override public void initialize() { /* nichts zu tun */ }

    private static <T> void push(Deque<T> deque, T value) {
        deque.addFirst(value);
        while (deque.size() > MAX_ENTRIES) deque.pollLast();
    }

    private static <T> List<T> head(Deque<T> deque, int limit) {
        List<T> out = new ArrayList<>(Math.min(limit, deque.size()));
        for (T t : deque) {
            if (out.size() >= limit) break;
            out.add(t);
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public PerformanceRepository performance() {
        return new PerformanceRepository() {
            @Override public void save(PerformanceSnapshot s) { push(perf, s); }
            @Override public List<PerformanceSnapshot> recent(int limit) { return head(perf, limit); }
            @Override public Optional<PerformanceSnapshot> latest() { return Optional.ofNullable(perf.peekFirst()); }
        };
    }

    @Override
    public ConflictRepository conflicts() {
        return new ConflictRepository() {
            @Override public void save(Instant at, ConflictReport c) { push(conflictList, c); }
            @Override public List<ConflictReport> recent(int limit) { return head(conflictList, limit); }
        };
    }

    @Override
    public SecurityRepository security() {
        return new SecurityRepository() {
            @Override public void save(Instant at, SecurityRisk r) { push(riskList, r); }
            @Override public List<SecurityRisk> recent(int limit) { return head(riskList, limit); }
        };
    }

    @Override
    public RecommendationRepository recommendations() {
        return new RecommendationRepository() {
            @Override public void save(Instant at, Recommendation r) { push(recList, r); }
            @Override public List<Recommendation> recent(int limit) { return head(recList, limit); }
        };
    }

    @Override
    public PluginRepository plugins() {
        return new PluginRepository() {
            @Override public void saveInventory(Instant at, List<PluginInfo> plugins) { inventory = List.copyOf(plugins); }
            @Override public List<PluginInfo> latestInventory() { return inventory; }
        };
    }

    @Override public void close() { /* nichts zu tun */ }
}
