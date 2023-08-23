package me.hash.mediaroulette.utils;

import com.mongodb.client.MongoCollection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

import org.bson.Document;

public class Config {
    private final MongoCollection<Document> collection;

    public Config(Database db) {
        this.collection = db.getCollection("bot");
        // Create the configuration document if it does not exist
        if (collection.countDocuments(new Document("name", "config")) == 0) {
            collection.insertOne(new Document("name", "config"));
        }
    }

    public void set(String key, Object value) {
        Document update = new Document(key, value);
        collection.updateOne(new Document("name", "config"), new Document("$set", update));
    }

    public <T> T get(String key, Class<T> clazz) {
        Document config = collection.find(new Document("name", "config")).first();
        if (config != null) {
            return clazz.cast(config.get(key));
        } else {
            return null;
        }
    }

    public <T> T getOrDefault(String key, T defaultValue, Class<T> clazz) {
        Document config = collection.find(new Document("name", "config")).first();
        if (config != null && config.containsKey(key)) {
            return clazz.cast(config.get(key));
        } else {
            return defaultValue;
        }
    }

    public boolean exists(String key) {
        Document config = collection.find(new Document("name", "config")).first();
        return config != null && config.containsKey(key);
    }

    public void remove(String key) {
        collection.updateOne(new Document("name", "config"), new Document("$unset", new Document(key, "")));
    }

    public static String formatBigInteger(BigInteger value) {
        String[] units = new String[] { "", "k", "m", "b", "t" };
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
