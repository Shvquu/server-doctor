package com.serverdoctor.storage;

import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MemoryStorageProviderTest {

    private PerformanceSnapshot snap(double tps) {
        return new PerformanceSnapshot(new double[]{tps,tps,tps}, 5.0,
                new MemoryStats(1,1,1,0,0), 10, 0, Instant.now());
    }

    @Test void returnsMostRecentFirst() {
        StorageProvider store = StorageProviders.create(StorageConfig.memory());
        store.initialize();
        store.performance().save(snap(20.0));
        store.performance().save(snap(15.0));
        assertEquals(15.0, store.performance().latest().orElseThrow().tps1m());
        assertEquals(15.0, store.performance().recent(10).get(0).tps1m());
        assertEquals(2, store.performance().recent(10).size());
    }

    @Test void limitIsRespected() {
        StorageProvider store = StorageProviders.create(StorageConfig.memory());
        store.initialize();
        for (int i = 0; i < 5; i++) store.performance().save(snap(i));
        assertEquals(3, store.performance().recent(3).size());
    }

    @Test void storesConflicts() {
        StorageProvider store = StorageProviders.create(StorageConfig.memory());
        store.initialize();
        store.conflicts().save(Instant.now(), new ConflictReport("c","A","B",Severity.HIGH,"x"));
        assertEquals(1, store.conflicts().recent(10).size());
    }

    @Test void unsupportedBackendFailsClearly() {
        assertThrows(StorageException.class,
                () -> StorageProviders.create(new StorageConfig(StorageType.MARIADB, null, null, null)));
    }
}
