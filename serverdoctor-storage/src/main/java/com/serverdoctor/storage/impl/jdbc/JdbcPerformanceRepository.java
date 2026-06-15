package com.serverdoctor.storage.impl.jdbc;

import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.PerformanceRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class JdbcPerformanceRepository implements PerformanceRepository {

    private final JdbcContext ctx;
    JdbcPerformanceRepository(JdbcContext ctx) { this.ctx = ctx; }

    @Override
    public void save(PerformanceSnapshot s) {
        String sql = "INSERT INTO performance(captured_at,tps1m,tps5m,tps15m,mspt," +
                "mem_used,mem_committed,mem_max,gc_count,gc_time,threads,players) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        double[] tps = s.tps();
        try (Connection c = ctx.dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.capturedAt().toString());
            ps.setDouble(2, tps.length > 0 ? tps[0] : Double.NaN);
            ps.setDouble(3, tps.length > 1 ? tps[1] : Double.NaN);
            ps.setDouble(4, tps.length > 2 ? tps[2] : Double.NaN);
            ps.setDouble(5, s.mspt());
            ps.setLong(6, s.memory().usedBytes());
            ps.setLong(7, s.memory().committedBytes());
            ps.setLong(8, s.memory().maxBytes());
            ps.setLong(9, s.memory().gcCount());
            ps.setLong(10, s.memory().gcTimeMs());
            ps.setInt(11, s.threadCount());
            ps.setInt(12, s.onlinePlayers());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new StorageException("Konnte Performance-Snapshot nicht speichern", e);
        }
    }

    @Override
    public List<PerformanceSnapshot> recent(int limit) {
        String sql = "SELECT * FROM performance ORDER BY id DESC LIMIT ?";
        List<PerformanceSnapshot> out = new ArrayList<>();
        try (Connection c = ctx.dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
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

    private static PerformanceSnapshot map(ResultSet rs) throws Exception {
        double[] tps = { rs.getDouble("tps1m"), rs.getDouble("tps5m"), rs.getDouble("tps15m") };
        MemoryStats mem = new MemoryStats(rs.getLong("mem_used"), rs.getLong("mem_committed"),
                rs.getLong("mem_max"), rs.getLong("gc_count"), rs.getLong("gc_time"));
        return new PerformanceSnapshot(tps, rs.getDouble("mspt"), mem,
                rs.getInt("threads"), rs.getInt("players"),
                Instant.parse(rs.getString("captured_at")));
    }
}
