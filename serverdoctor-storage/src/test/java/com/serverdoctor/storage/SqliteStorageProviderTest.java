package com.serverdoctor.storage;

import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nutzt eine In-Memory-SQLite-Datenbank (jdbc:sqlite::memory:) - benötigt den
 * sqlite-jdbc-Treiber auf dem Test-Classpath (per testImplementation vorhanden).
 */
class SqliteStorageProviderTest {

    private StorageProvider open() {
        StorageProvider store = StorageProviders.create(StorageConfig.sqlite(":memory:"));
        store.initialize();
        return store;
    }

    @Test void roundTripsPerformanceSnapshot() {
        try (StorageProvider store = open()) {
            var mem = new MemoryStats(800, 900, 1000, 5, 50);
            store.performance().save(new PerformanceSnapshot(
                    new double[]{19.9, 20.0, 20.0}, 2.5, mem, 42, 7, Instant.now()));
            var latest = store.performance().latest().orElseThrow();
            assertEquals(19.9, latest.tps1m(), 0.0001);
            assertEquals(42, latest.threadCount());
            assertEquals(7, latest.onlinePlayers());
            assertEquals(1000, latest.memory().maxBytes());
        }
    }

    @Test void roundTripsConflict() {
        try (StorageProvider store = open()) {
            store.conflicts().save(Instant.now(),
                    new ConflictReport("perms", "LuckPerms", "PEX", Severity.CRITICAL, "two perms"));
            var list = store.conflicts().recent(10);
            assertEquals(1, list.size());
            assertEquals("LuckPerms", list.get(0).pluginA());
            assertEquals(Severity.CRITICAL, list.get(0).severity());
        }
    }
}
