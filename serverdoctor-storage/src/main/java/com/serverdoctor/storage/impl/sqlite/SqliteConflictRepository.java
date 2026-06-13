package com.serverdoctor.storage.impl.sqlite;

import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.ConflictRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class SqliteConflictRepository implements ConflictRepository {

    private final SqliteContext ctx;
    SqliteConflictRepository(SqliteContext ctx) { this.ctx = ctx; }

    @Override
    public void save(Instant at, ConflictReport c) {
        String sql = "INSERT INTO conflicts(at,conflict_id,plugin_a,plugin_b,severity,description) VALUES(?,?,?,?,?,?)";
        synchronized (ctx.lock) {
            try (PreparedStatement ps = ctx.connection.prepareStatement(sql)) {
                ps.setString(1, at.toString());
                ps.setString(2, c.id());
                ps.setString(3, c.pluginA());
                ps.setString(4, c.pluginB());
                ps.setString(5, c.severity().name());
                ps.setString(6, c.description());
                ps.executeUpdate();
            } catch (Exception e) {
                throw new StorageException("Konnte Konflikt nicht speichern", e);
            }
        }
    }

    @Override
    public List<ConflictReport> recent(int limit) {
        String sql = "SELECT * FROM conflicts ORDER BY id DESC LIMIT ?";
        List<ConflictReport> out = new ArrayList<>();
        synchronized (ctx.lock) {
            try (PreparedStatement ps = ctx.connection.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new ConflictReport(rs.getString("conflict_id"), rs.getString("plugin_a"),
                                rs.getString("plugin_b"), Severity.valueOf(rs.getString("severity")),
                                rs.getString("description")));
                    }
                }
            } catch (Exception e) {
                throw new StorageException("Konnte Konflikt-Historie nicht lesen", e);
            }
        }
        return out;
    }
}
