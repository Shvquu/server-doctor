package com.serverdoctor.storage.impl.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/** Hält MongoClient + Datenbank und liefert die Collections. */
final class MongoContext {

    final MongoClient client;
    final MongoDatabase database;

    MongoContext(MongoClient client, MongoDatabase database) {
        this.client = client;
        this.database = database;
    }

    MongoCollection<Document> collection(String name) {
        return database.getCollection(name);
    }
}
