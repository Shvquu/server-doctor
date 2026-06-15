package com.serverdoctor.storage.impl.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.SecurityRepository;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class MongoSecurityRepository implements SecurityRepository {

    private final MongoCollection<Document> col;
    MongoSecurityRepository(MongoContext ctx) { this.col = ctx.collection("security_risks"); }

    @Override
    public void save(Instant at, SecurityRisk r) {
        Document doc = new Document("at", at.toString())
                .append("plugin_name", r.pluginName())
                .append("type", r.type().name())
                .append("severity", r.severity().name())
                .append("description", r.description());
        try {
            col.insertOne(doc);
        } catch (Exception e) {
            throw new StorageException("Konnte Sicherheitsrisiko nicht speichern", e);
        }
    }

    @Override
    public List<SecurityRisk> recent(int limit) {
        List<SecurityRisk> out = new ArrayList<>();
        try {
            for (Document d : col.find().sort(Sorts.descending("_id")).limit(limit)) {
                out.add(new SecurityRisk(d.getString("plugin_name"),
                        SecurityRisk.RiskType.valueOf(d.getString("type")),
                        Severity.valueOf(d.getString("severity")),
                        d.getString("description")));
            }
        } catch (Exception e) {
            throw new StorageException("Konnte Sicherheits-Historie nicht lesen", e);
        }
        return out;
    }
}
