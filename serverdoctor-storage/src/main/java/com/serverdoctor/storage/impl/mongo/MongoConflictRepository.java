package com.serverdoctor.storage.impl.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.ConflictRepository;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class MongoConflictRepository implements ConflictRepository {

    private final MongoCollection<Document> col;
    MongoConflictRepository(MongoContext ctx) { this.col = ctx.collection("conflicts"); }

    @Override
    public void save(Instant at, ConflictReport c) {
        Document doc = new Document("at", at.toString())
                .append("conflict_id", c.id())
                .append("plugin_a", c.pluginA())
                .append("plugin_b", c.pluginB())
                .append("severity", c.severity().name())
                .append("description", c.description());
        try {
            col.insertOne(doc);
        } catch (Exception e) {
            throw new StorageException("Konnte Konflikt nicht speichern", e);
        }
    }

    @Override
    public List<ConflictReport> recent(int limit) {
        List<ConflictReport> out = new ArrayList<>();
        try {
            for (Document d : col.find().sort(Sorts.descending("_id")).limit(limit)) {
                out.add(new ConflictReport(d.getString("conflict_id"), d.getString("plugin_a"),
                        d.getString("plugin_b"), Severity.valueOf(d.getString("severity")),
                        d.getString("description")));
            }
        } catch (Exception e) {
            throw new StorageException("Konnte Konflikt-Historie nicht lesen", e);
        }
        return out;
    }
}
