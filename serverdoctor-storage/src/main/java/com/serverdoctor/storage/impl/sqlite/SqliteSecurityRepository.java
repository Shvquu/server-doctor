package com.serverdoctor.storage.impl.sqlite;

import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.SecurityRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class SqliteSecurityRepository implements SecurityRepository {

    private final SqliteContext ctx;
    SqliteSecurityRepository(SqliteContext ctx) { this.ctx = ctx; }

    @Override
    public void save(Instant at, SecurityRisk r) {
        String sql = "INSERT INTO security_risks(at,plugin_name,type,severity,description) VALUES(?,?,?,?,?)";
        synchronized (ctx.lock) {
            try (PreparedStatement ps = ctx.connection.prepareStatement(sql)) {
                ps.setString(1, at.toString());
                ps.setString(2, r.pluginName());
                ps.setString(3, r.type().name());
                ps.setString(4, r.severity().name());
                ps.setString(5, r.description());
                ps.executeUpdate();
            } catch (Exception e) {
                throw new StorageException("Konnte Sicherheitsrisiko nicht speichern", e);
            }
        }
    }

    @Override
    public List<SecurityRisk> recent(int limit) {
        String sql = "SELECT * FROM security_risks ORDER BY id DESC LIMIT ?";
        List<SecurityRisk> out = new ArrayList<>();
        synchronized (ctx.lock) {
            try (PreparedStatement ps = ctx.connection.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new SecurityRisk(rs.getString("plugin_name"),
                                SecurityRisk.RiskType.valueOf(rs.getString("type")),
                                Severity.valueOf(rs.getString("severity")),
                                rs.getString("description")));
                    }
                }
            } catch (Exception e) {
                throw new StorageException("Konnte Sicherheits-Historie nicht lesen", e);
            }
        }
        return out;
    }
}
