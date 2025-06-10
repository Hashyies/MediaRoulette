package me.hash.mediaroulette.repository;

import com.mongodb.client.MongoCollection;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.model.Favorite;
import me.hash.mediaroulette.model.ImageOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MongoUserRepository implements UserRepository {
    private final MongoCollection<Document> userCollection;

    public MongoUserRepository(MongoCollection<Document> userCollection) {
        this.userCollection = userCollection;
    }

    @Override
    public Optional<User> findById(String userId) {
        Document doc = userCollection.find(new Document("_id", userId)).first();
        if (doc != null) {
            User user = mapDocumentToUser(doc);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public User save(User user) {
        Document doc = mapUserToDocument(user);
        userCollection.replaceOne(new Document("_id", user.getUserId()), doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true));
        return user;
    }

    @Override
    public boolean exists(String userId) {
        return userCollection.find(new Document("_id", userId)).first() != null;
    }

    // --- Helper Methods for Mapping ---

    private User mapDocumentToUser(Document doc) {
        String userId = doc.getString("_id");
        User user = new User(userId);
        user.setImagesGenerated(doc.getLong("imagesGenerated") != null ? doc.getLong("imagesGenerated") : 0L);
        user.setNsfw(doc.getBoolean("nsfw", false));
        user.setPremium(doc.getBoolean("premium", false));
        user.setAdmin(doc.getBoolean("admin", false));
        user.setLocale(doc.getString("locale") != null ? doc.getString("locale") : "en_US");

        // Map favorites (assumes favorites is stored as a List of Documents)
        List<Document> favDocs = (List<Document>) doc.get("favorites", new ArrayList<Document>());
        for (Document favDoc : favDocs) {
            int id = favDoc.getInteger("id", 0);
            String description = favDoc.getString("description");
            String image = favDoc.getString("image");
            String type = favDoc.getString("type");
            user.getFavorites().add(new Favorite(id, description, image, type));
        }

        // Map image options (assumes a sub-document "images")
        Document imagesDoc = (Document) doc.get("images");
        if (imagesDoc != null) {
            for (String key : imagesDoc.keySet()) {
                Document optionDoc = (Document) imagesDoc.get(key);
                boolean enabled = optionDoc.getBoolean("enabled", false);
                double chance = optionDoc.getDouble("chance") != null ? optionDoc.getDouble("chance") : 0.0;
                user.getImageOptionsMap().put(key, new ImageOptions(key, enabled, chance));
            }
        }
        return user;
    }

    private Document mapUserToDocument(User user) {
        Document doc = new Document("_id", user.getUserId())
                .append("imagesGenerated", user.getImagesGenerated())
                .append("nsfw", user.isNsfw())
                .append("premium", user.isPremium())
                .append("admin", user.isAdmin())
                .append("locale", user.getLocale());

        // Map favorites
        List<Document> favDocs = new ArrayList<>();
        for (Favorite fav : user.getFavorites()) {
            Document favDoc = new Document("id", fav.getId())
                    .append("description", fav.getDescription())
                    .append("image", fav.getImage())
                    .append("type", fav.getType());
            favDocs.add(favDoc);
        }
        doc.append("favorites", favDocs);

        // Map image options
        Document imagesDoc = new Document();
        for (Map.Entry<String, ImageOptions> entry : user.getImageOptionsMap().entrySet()) {
            ImageOptions option = entry.getValue();
            Document optionDoc = new Document("enabled", option.isEnabled())
                    .append("chance", option.getChance());
            imagesDoc.append(entry.getKey(), optionDoc);
        }
        doc.append("images", imagesDoc);
        return doc;
    }
}
