package com.serverdoctor.storage.impl.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.RecommendationRepository;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class MongoRecommendationRepository implements RecommendationRepository {

    private final MongoCollection<Document> col;
    MongoRecommendationRepository(MongoContext ctx) { this.col = ctx.collection("recommendations"); }

    @Override
    public void save(Instant at, Recommendation r) {
        Document doc = new Document("at", at.toString())
                .append("rec_id", r.id())
                .append("category", r.category().name())
                .append("severity", r.severity().name())
                .append("title", r.title())
                .append("description", r.description());
        try {
            col.insertOne(doc);
        } catch (Exception e) {
            throw new StorageException("Konnte Empfehlung nicht speichern", e);
        }
    }

    @Override
    public List<Recommendation> recent(int limit) {
        List<Recommendation> out = new ArrayList<>();
        try {
            for (Document d : col.find().sort(Sorts.descending("_id")).limit(limit)) {
                out.add(new Recommendation(d.getString("rec_id"),
                        Recommendation.Category.valueOf(d.getString("category")),
                        Severity.valueOf(d.getString("severity")),
                        d.getString("title"), d.getString("description")));
            }
        } catch (Exception e) {
            throw new StorageException("Konnte Empfehlungs-Historie nicht lesen", e);
        }
        return out;
    }
}
