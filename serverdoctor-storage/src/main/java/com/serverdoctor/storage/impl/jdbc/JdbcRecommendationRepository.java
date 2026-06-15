package com.serverdoctor.storage.impl.jdbc;

import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.RecommendationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class JdbcRecommendationRepository implements RecommendationRepository {

    private final JdbcContext ctx;
    JdbcRecommendationRepository(JdbcContext ctx) { this.ctx = ctx; }

    @Override
    public void save(Instant at, Recommendation r) {
        String sql = "INSERT INTO recommendations(at,rec_id,category,severity,title,description) VALUES(?,?,?,?,?,?)";
        try (Connection con = ctx.dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, at.toString());
            ps.setString(2, r.id());
            ps.setString(3, r.category().name());
            ps.setString(4, r.severity().name());
            ps.setString(5, r.title());
            ps.setString(6, r.description());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new StorageException("Konnte Empfehlung nicht speichern", e);
        }
    }

    @Override
    public List<Recommendation> recent(int limit) {
        String sql = "SELECT * FROM recommendations ORDER BY id DESC LIMIT ?";
        List<Recommendation> out = new ArrayList<>();
        try (Connection con = ctx.dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Recommendation(rs.getString("rec_id"),
                            Recommendation.Category.valueOf(rs.getString("category")),
                            Severity.valueOf(rs.getString("severity")),
                            rs.getString("title"), rs.getString("description")));
                }
            }
        } catch (Exception e) {
            throw new StorageException("Konnte Empfehlungs-Historie nicht lesen", e);
        }
        return out;
    }
}
