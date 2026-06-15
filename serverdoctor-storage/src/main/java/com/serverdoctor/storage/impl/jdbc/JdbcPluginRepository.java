package com.serverdoctor.storage.impl.jdbc;

import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.PluginRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class JdbcPluginRepository implements PluginRepository {

    private final JdbcContext ctx;
    JdbcPluginRepository(JdbcContext ctx) { this.ctx = ctx; }

    @Override
    public void saveInventory(Instant at, List<PluginInfo> plugins) {
        String sql = "INSERT INTO plugin_inventory(at,name,version,authors,enabled) VALUES(?,?,?,?,?)";
        try (Connection con = ctx.dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String ts = at.toString();
            for (PluginInfo p : plugins) {
                ps.setString(1, ts);
                ps.setString(2, p.name());
                ps.setString(3, p.version());
                ps.setString(4, String.join(",", p.authors()));
                ps.setInt(5, p.enabled() ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            throw new StorageException("Konnte Plugin-Inventar nicht speichern", e);
        }
    }

    @Override
    public List<PluginInfo> latestInventory() {
        String sql = "SELECT name,version,authors,enabled FROM plugin_inventory " +
                "WHERE at = (SELECT at FROM plugin_inventory ORDER BY id DESC LIMIT 1)";
        List<PluginInfo> out = new ArrayList<>();
        try (Connection con = ctx.dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String authors = rs.getString("authors");
                List<String> authorList = authors == null || authors.isBlank()
                        ? List.of() : List.of(authors.split(","));
                out.add(new PluginInfo(rs.getString("name"), rs.getString("version"),
                        authorList, List.of(), List.of(), rs.getInt("enabled") == 1));
            }
        } catch (Exception e) {
            throw new StorageException("Konnte Plugin-Inventar nicht lesen", e);
        }
        return out;
    }
}
