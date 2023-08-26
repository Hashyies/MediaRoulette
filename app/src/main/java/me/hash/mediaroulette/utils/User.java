package me.hash.mediaroulette.utils;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import java.util.HashMap;
import java.util.Map;

public class User {
    private final Database db;
    private final String userId;
    private final Document userData;

    public User(Database db, String userId) {
        this.db = db;
        this.userId = userId;

        // Check if the user collection exists
        if (!db.collectionExists("user")) {
            db.createCollection("user");
        }

        // Check if the user exists in the database
        MongoCollection<Document> userCollection = db.getCollection("user");
        Document userDoc = userCollection.find(new Document("_id", userId)).first();
        if (userDoc == null) {
            // User does not exist, create a new user document
            userDoc = new Document("_id", userId);
            userDoc.append("images", new Document());
            userCollection.insertOne(userDoc);
        }

        this.userData = userDoc;
    }

    public boolean exists() {
        return userData != null;
    }

    public Map<String, ImageOptions> getAllImageOptions() {
        Map<String, ImageOptions> allImageOptions = new HashMap<>();
        Document images = (Document) userData.get("images");
        for (String imageType : images.keySet()) {
            Document imageOptionsData = (Document) images.get(imageType);
            allImageOptions.put(imageType, new ImageOptions(imageType, imageOptionsData.getBoolean("enabled"), imageOptionsData.getDouble("chance")));
        }
        return allImageOptions;
    }

    public ImageOptions getImageOptions(String imageType) {
        Document images = (Document) userData.get("images");
        Document imageOptionsData = (Document) images.get(imageType);
        if (imageOptionsData == null) {
            return null;
        }
        return new ImageOptions(imageType, imageOptionsData.getBoolean("enabled"), imageOptionsData.getDouble("chance"));
    }

    public void setChances(ImageOptions... options) {
        MongoCollection<Document> userCollection = db.getCollection("user");
        Document images = (Document) userData.get("images");
        for (ImageOptions option : options) {
            images.append(option.getImageType(), new Document()
                    .append("enabled", option.isEnabled())
                    .append("chance", option.getChance()));
        }
        userCollection.updateOne(new Document("_id", userId), new Document("$set", new Document("images", images)));
    }
}
