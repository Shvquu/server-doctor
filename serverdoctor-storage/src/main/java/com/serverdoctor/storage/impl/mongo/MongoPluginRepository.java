package com.serverdoctor.storage.impl.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.repository.PluginRepository;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Speichert je Inventar-Aufnahme ein Dokument mit eingebettetem {@code plugins}-Array.
 * {@link #latestInventory()} liefert das jüngste Dokument (nach {@code _id}).
 */
final class MongoPluginRepository implements PluginRepository {

    private final MongoCollection<Document> col;
    MongoPluginRepository(MongoContext ctx) { this.col = ctx.collection("plugin_inventory"); }

    @Override
    public void saveInventory(Instant at, List<PluginInfo> plugins) {
        List<Document> docs = new ArrayList<>();
        for (PluginInfo p : plugins) {
            docs.add(new Document("name", p.name())
                    .append("version", p.version())
                    .append("authors", new ArrayList<>(p.authors()))
                    .append("enabled", p.enabled()));
        }
        Document snapshot = new Document("at", at.toString()).append("plugins", docs);
        try {
            col.insertOne(snapshot);
        } catch (Exception e) {
            throw new StorageException("Konnte Plugin-Inventar nicht speichern", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PluginInfo> latestInventory() {
        List<PluginInfo> out = new ArrayList<>();
        try {
            Document latest = col.find().sort(Sorts.descending("_id")).first();
            if (latest == null) return out;
            List<Document> plugins = (List<Document>) latest.get("plugins");
            if (plugins == null) return out;
            for (Document p : plugins) {
                List<String> authors = (List<String>) p.get("authors");
                out.add(new PluginInfo(p.getString("name"), p.getString("version"),
                        authors == null ? List.of() : authors,
                        List.of(), List.of(), p.getBoolean("enabled", false)));
            }
        } catch (Exception e) {
            throw new StorageException("Konnte Plugin-Inventar nicht lesen", e);
        }
        return out;
    }
}
