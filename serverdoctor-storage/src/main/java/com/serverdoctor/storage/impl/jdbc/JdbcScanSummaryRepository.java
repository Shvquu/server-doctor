package com.serverdoctor.storage.impl.jdbc;

import com.serverdoctor.common.model.ScanSummary;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.ScanSummaryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class JdbcScanSummaryRepository implements ScanSummaryRepository {

    private final JdbcContext ctx;
    JdbcScanSummaryRepository(JdbcContext ctx) { this.ctx = ctx; }

    @Override
    public void save(ScanSummary s) {
        String sql = "INSERT INTO scan_summaries(at,findings,severe_findings,conflicts,security_risks) VALUES(?,?,?,?,?)";
        try (Connection con = ctx.dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.at().toString());
            ps.setInt(2, s.findings());
            ps.setInt(3, s.severeFindings());
            ps.setInt(4, s.conflicts());
            ps.setInt(5, s.securityRisks());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new StorageException("Konnte Scan-Zusammenfassung nicht speichern", e);
        }
    }

    @Override
    public List<ScanSummary> recent(int limit) {
        String sql = "SELECT * FROM scan_summaries ORDER BY id DESC LIMIT ?";
        List<ScanSummary> out = new ArrayList<>();
        try (Connection con = ctx.dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ScanSummary(Instant.parse(rs.getString("at")), rs.getInt("findings"),
                            rs.getInt("severe_findings"), rs.getInt("conflicts"),
                            rs.getInt("security_risks")));
                }
            }
        } catch (Exception e) {
            throw new StorageException("Konnte Scan-Historie nicht lesen", e);
        }
        return out;
    }
}
