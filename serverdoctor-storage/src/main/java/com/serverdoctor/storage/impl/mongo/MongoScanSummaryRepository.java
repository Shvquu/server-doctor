package com.serverdoctor.storage.impl.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.serverdoctor.common.model.ScanSummary;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.ScanSummaryRepository;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class MongoScanSummaryRepository implements ScanSummaryRepository {

    private final MongoCollection<Document> col;
    MongoScanSummaryRepository(MongoContext ctx) { this.col = ctx.collection("scan_summaries"); }

    @Override
    public void save(ScanSummary s) {
        Document doc = new Document("at", s.at().toString())
                .append("findings", s.findings())
                .append("severe_findings", s.severeFindings())
                .append("conflicts", s.conflicts())
                .append("security_risks", s.securityRisks());
        try {
            col.insertOne(doc);
        } catch (Exception e) {
            throw new StorageException("Konnte Scan-Zusammenfassung nicht speichern", e);
        }
    }

    @Override
    public List<ScanSummary> recent(int limit) {
        List<ScanSummary> out = new ArrayList<>();
        try {
            for (Document d : col.find().sort(Sorts.descending("_id")).limit(limit)) {
                out.add(new ScanSummary(Instant.parse(d.getString("at")), d.getInteger("findings", 0),
                        d.getInteger("severe_findings", 0), d.getInteger("conflicts", 0),
                        d.getInteger("security_risks", 0)));
            }
        } catch (Exception e) {
            throw new StorageException("Konnte Scan-Historie nicht lesen", e);
        }
        return out;
    }
}
