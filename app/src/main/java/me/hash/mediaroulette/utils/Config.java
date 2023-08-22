package me.hash.mediaroulette.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

import org.bson.Document;

public class Config {
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;

    public Config(String connectionString, String databaseName) {
        this.mongoClient = MongoClients.create(connectionString);
        this.database = mongoClient.getDatabase(databaseName);
        this.collection = database.getCollection("bot");
        // Create the configuration document if it does not exist
        if (collection.countDocuments(new Document("config", true)) == 0) {
            collection.insertOne(new Document("config", true));
        }
    }

    public void set(String key, Object value) {
        Document update = new Document(key, value);
        collection.updateOne(new Document("config", true), new Document("$set", update));
    }

    public <T> T get(String key, Class<T> clazz) {
        Document config = collection.find(new Document("config", true)).first();
        if (config != null) {
            return clazz.cast(config.get(key));
        } else {
            return null;
        }
    }

    public <T> T getOrDefault(String key, T defaultValue, Class<T> clazz) {
        Document config = collection.find(new Document("config", true)).first();
        if (config != null && config.containsKey(key)) {
            return clazz.cast(config.get(key));
        } else {
            return defaultValue;
        }
    }
    
    

    public boolean exists(String key) {
        Document config = collection.find(new Document("config", true)).first();
        return config != null && config.containsKey(key);
    }

    public void remove(String key) {
        collection.updateOne(new Document("config", true), new Document("$unset", new Document(key, "")));
    }

    public static String formatBigInteger(BigInteger value) {
        String[] units = new String[] { "", "k", "m", "b", "t", "unit1", "unit2", "unit3" };
        int unitIndex = 0;
        BigDecimal decimalValue = new BigDecimal(value);
        while (decimalValue.compareTo(BigDecimal.valueOf(1000)) >= 0 && unitIndex < units.length - 1) {
            decimalValue = decimalValue.divide(BigDecimal.valueOf(1000));
            unitIndex++;
        }
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.##");
        return decimalFormat.format(decimalValue) + units[unitIndex];
    }

}