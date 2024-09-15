package me.hash.mediaroulette.utils.user;

import com.mongodb.client.MongoCollection;
import me.hash.mediaroulette.utils.Database;
import me.hash.mediaroulette.utils.GlobalLogger;
import me.hash.mediaroulette.utils.ImageOptions;
import me.hash.mediaroulette.utils.exceptions.InvalidChancesException;
import me.hash.mediaroulette.utils.exceptions.NoEnabledOptionsException;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class User {
    private static final ConcurrentHashMap<String, User> cache = new ConcurrentHashMap<>();
    private final Database db;
    private final String userId;
    private Document userData;
    private static final int DEFAULT_FAVORITE_LIMIT = 25;
    private static final Logger logger = GlobalLogger.getLogger();

    private User(Database db, String userId) {
        this.db = db;
        this.userId = userId;
        initializeUser();
    }

    // Create or fetch a user and cache it.
    public static User get(Database db, String userId) {
        return cache.computeIfAbsent(userId, id -> new User(db, id));
    }

    // Initialize user from database or create a new one.
    private void initializeUser() {
        MongoCollection<Document> userCollection = db.getCollection("user");

        if (!db.collectionExists("user")) {
            logger.log(Level.INFO, "Creating 'user' collection.");
            db.createCollection("user");
        }

        userData = Optional.ofNullable(userCollection.find(new Document("_id", userId)).first())
                .orElseGet(() -> createNewUser(userCollection));
    }

    // Create new user in DB and log.
    private Document createNewUser(MongoCollection<Document> userCollection) {
        Document userDoc = new Document("_id", userId)
                .append("imagesGenerated", 0L)
                .append("images", new Document())
                .append("nsfw", false)
                .append("favorites", new ArrayList<Document>())
                .append("premium", false)
                .append("admin", false)
                .append("favoriteLimit", DEFAULT_FAVORITE_LIMIT);
        userCollection.insertOne(userDoc);
        logger.log(Level.INFO, "Created new user document for userId: {0}", userId);
        return userDoc;
    }

    // Immediately persist user data to the database.
    private void updateDatabase() {
        MongoCollection<Document> userCollection = db.getCollection("user");
        userCollection.updateOne(new Document("_id", userId), new Document("$set", userData));
        logger.log(Level.INFO, "Updated database for userId: {0}", userId);
    }

    // Methods to handle dirty writes after modification.
    public void incrementImagesGenerated() {
        long newCount = getImagesGenerated() + 1;
        userData.put("imagesGenerated", newCount);
        logger.log(Level.INFO, "Incremented image count for userId: {0} to {1}", new Object[]{userId, newCount});
        updateDatabase();
    }

    public void setNsfwEnabled(boolean enabled) {
        userData.put("nsfw", enabled);
        logger.log(Level.INFO, "Set NSFW status for userId: {0} to {1}", new Object[]{userId, enabled});
        updateDatabase();
    }

    public void setPremium(boolean premium) {
        userData.put("premium", premium);
        logger.log(Level.INFO, "Set premium status for userId: {0} to {1}", new Object[]{userId, premium});
        updateDatabase();
    }

    public void setAdmin(boolean admin) {
        userData.put("admin", admin);
        logger.log(Level.INFO, "Set admin status for userId: {0} to {1}", new Object[]{userId, admin});
        updateDatabase();
    }

    public void addFavorite(String description, String image, String type) {
        List<Document> favorites = getFavoritesList();
        if (favorites.size() >= getFavoriteLimit()) {
            logger.log(Level.WARNING, "Favorite limit reached for userId: {0}. Cannot add more favorites.", userId);
            return;
        }
        int id = favorites.size();
        favorites.add(new Document()
                .append("id", id)
                .append("description", description)
                .append("image", image)
                .append("type", type));
        userData.put("favorites", favorites);
        logger.log(Level.INFO, "Added favorite for userId: {0}", userId);
        updateDatabase();
    }

    public void removeFavorite(int id) {
        List<Document> favorites = getFavoritesList();
        if (id >= 0 && id < favorites.size()) {
            favorites.remove(id);
            for (int i = id; i < favorites.size(); i++) {
                favorites.get(i).put("id", i);
            }
        }
        userData.put("favorites", favorites);
        logger.log(Level.INFO, "Removed favorite for userId: {0}, favoriteId: {1}", new Object[]{userId, id});
        updateDatabase();
    }

    public Map<String, ImageOptions> getAllImageOptions() {
        Map<String, ImageOptions> allImageOptions = new HashMap<>();
        Document images = (Document) userData.get("images");
        for (String imageType : images.keySet()) {
            Document imageOptionsData = (Document) images.get(imageType);
            allImageOptions.put(imageType, new ImageOptions(imageType, imageOptionsData.getBoolean("enabled"),
                    imageOptionsData.getDouble("chance")));
        }
        return allImageOptions;
    }

    public int getFavoriteLimit() {
        return isPremium() ?
                userData.getInteger("favoriteLimit", DEFAULT_FAVORITE_LIMIT) * 2 :
                userData.getInteger("favoriteLimit", DEFAULT_FAVORITE_LIMIT);
    }

    public void setChances(ImageOptions... options) {
        Document images = Optional.ofNullable(userData.get("images", Document.class)).orElse(new Document());
        Arrays.stream(options).forEach(option -> images.put(option.getImageType(), new Document()
                .append("enabled", option.isEnabled())
                .append("chance", option.getChance())));
        userData.put("images", images);
        logger.log(Level.INFO, "Updated image options for userId: {0}", userId);
    }

    public ImageOptions getImageOptions(String imageType) {
        Document images = userData.get("images", Document.class);
        Document imageOptionsData = images != null ? (Document) images.get(imageType) : null;
        return imageOptionsData == null ? null : new ImageOptions(imageType, imageOptionsData.getBoolean("enabled"), imageOptionsData.getDouble("chance"));
    }

    public boolean exists() { return userData != null; }
    public long getImagesGenerated() { return userData.getLong("imagesGenerated"); }
    public boolean isNsfwEnabled() { return userData.getBoolean("nsfw", false); }
    public boolean isPremium() { return userData.getBoolean("premium", false); }
    public boolean isAdmin() { return userData.getBoolean("admin", false); }
    public List<Document> getFavorites() { return getFavoritesList(); }
    @SuppressWarnings("unchecked")
    private List<Document> getFavoritesList() { return userData.get("favorites", ArrayList.class); }


    public Map<String, String> getImage() throws NoEnabledOptionsException, InvalidChancesException {
        ImageSelector imageSelector = new ImageSelector(getAllImageOptions());
        return imageSelector.selectImage();
    }
}
