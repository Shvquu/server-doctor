package com.serverdoctor.storage.impl.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.PerformanceRepository;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class MongoPerformanceRepository implements PerformanceRepository {

    private final MongoCollection<Document> col;
    MongoPerformanceRepository(MongoContext ctx) { this.col = ctx.collection("performance"); }

    @Override
    public void save(PerformanceSnapshot s) {
        double[] tps = s.tps();
        Document doc = new Document("captured_at", s.capturedAt().toString())
                .append("tps1m", tps.length > 0 ? tps[0] : Double.NaN)
                .append("tps5m", tps.length > 1 ? tps[1] : Double.NaN)
                .append("tps15m", tps.length > 2 ? tps[2] : Double.NaN)
                .append("mspt", s.mspt())
                .append("mem_used", s.memory().usedBytes())
                .append("mem_committed", s.memory().committedBytes())
                .append("mem_max", s.memory().maxBytes())
                .append("gc_count", s.memory().gcCount())
                .append("gc_time", s.memory().gcTimeMs())
                .append("threads", s.threadCount())
                .append("players", s.onlinePlayers());
        try {
            col.insertOne(doc);
        } catch (Exception e) {
            throw new StorageException("Konnte Performance-Snapshot nicht speichern", e);
        }
    }

    @Override
    public List<PerformanceSnapshot> recent(int limit) {
        List<PerformanceSnapshot> out = new ArrayList<>();
        try {
            for (Document d : col.find().sort(Sorts.descending("_id")).limit(limit)) {
                out.add(map(d));
            }
        } catch (Exception e) {
            throw new StorageException("Konnte Performance-Historie nicht lesen", e);
        }
        return out;
    }

    @Override
    public Optional<PerformanceSnapshot> latest() {
        List<PerformanceSnapshot> r = recent(1);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    private static PerformanceSnapshot map(Document d) {
        double[] tps = { num(d, "tps1m"), num(d, "tps5m"), num(d, "tps15m") };
        MemoryStats mem = new MemoryStats(lng(d, "mem_used"), lng(d, "mem_committed"),
                lng(d, "mem_max"), lng(d, "gc_count"), lng(d, "gc_time"));
        return new PerformanceSnapshot(tps, num(d, "mspt"), mem,
                d.getInteger("threads", 0), d.getInteger("players", 0),
                Instant.parse(d.getString("captured_at")));
    }

    private static double num(Document d, String key) {
        Number n = d.get(key, Number.class);
        return n == null ? Double.NaN : n.doubleValue();
    }

    private static long lng(Document d, String key) {
        Number n = d.get(key, Number.class);
        return n == null ? 0L : n.longValue();
    }
}
