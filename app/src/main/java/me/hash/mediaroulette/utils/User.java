package me.hash.mediaroulette.utils;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import me.hash.mediaroulette.utils.exceptions.InvalidChancesException;
import me.hash.mediaroulette.utils.exceptions.NoEnabledOptionsException;
import me.hash.mediaroulette.utils.random.RandomImage;
import me.hash.mediaroulette.utils.random.RandomMedia;
import me.hash.mediaroulette.utils.random.RandomReddit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

public class User {
    private static final ConcurrentHashMap<String, User> cache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Database db;
    private final String userId;
    private Document userData;
    private static final int DEFAULT_FAVORITE_LIMIT = 25;

    static {
        executor.scheduleAtFixedRate(() -> {
            for (User user : cache.values()) {
                user.updateDatabase();
            }
            cache.clear();
        }, 15, 15, TimeUnit.MINUTES);
    }

    public static User get(Database db, String userId) {
        return cache.computeIfAbsent(userId, id -> new User(db, id));
    }

    private User(Database db, String userId) {
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
            userDoc.append("nsfw", false);
            userDoc.append("favorites", null);
            userDoc.append("favoriteLimit", DEFAULT_FAVORITE_LIMIT);
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
            allImageOptions.put(imageType, new ImageOptions(imageType, imageOptionsData.getBoolean("enabled"),
                    imageOptionsData.getDouble("chance")));
        }
        return allImageOptions;
    }

    public ImageOptions getImageOptions(String imageType) {
        Document images = (Document) userData.get("images");
        Document imageOptionsData = (Document) images.get(imageType);
        if (imageOptionsData == null) {
            return null;
        }
        return new ImageOptions(imageType, imageOptionsData.getBoolean("enabled"),
                imageOptionsData.getDouble("chance"));
    }

    public void setChances(ImageOptions... options) {
        Document images = (Document) userData.get("images");
        for (ImageOptions option : options) {
            images.append(option.getImageType(), new Document()
                    .append("enabled", option.isEnabled())
                    .append("chance", option.getChance()));
        }
        userData.append("images", images);
    }

    public void setNsfwEnabled(boolean enabled) {
        userData.append("nsfw", enabled);
    }

    public boolean isNsfwEnabled() {
        return userData.getBoolean("nsfw");
    }

    public void addFavorite(String description, String image, String type) {
        List<Document> favorites = (List<Document>) userData.getOrDefault("favorites", new ArrayList<>());
        if (favorites.size() >= getFavoriteLimit()) {
            System.out.println("Favorite limit reached. Cannot add more favorites.");
            return;
        }
        int id = favorites.size();
        favorites.add(new Document()
                .append("id", id)
                .append("description", description)
                .append("image", image)
                .append("type", type));
        userData.append("favorites", favorites);
    }

    public void removeFavorite(int id) {
        List<Document> favorites = (List<Document>) userData.getOrDefault("favorites", new ArrayList<>());
        if (id >= 0 && id < favorites.size()) {
            favorites.remove(id);
            // Update IDs
            for (int i = id; i < favorites.size(); i++) {
                Document favorite = favorites.get(i);
                favorite.append("id", i);
            }
        }
        userData.append("favorites", favorites);
    }

    public Document getFavorite(int id) {
        List<Document> favorites = (List<Document>) userData.getOrDefault("favorites", new ArrayList<>());
        if (id >= 0 && id < favorites.size()) {
            return favorites.get(id);
        }
        return null;
    }

    public List<Document> getFavorites() {
        return (List<Document>) userData.getOrDefault("favorites", new ArrayList<>());
    }

    public void setFavoriteLimit(int limit) {
        userData.append("favoriteLimit", limit);
    }

    public int getFavoriteLimit() {
        return userData.getInteger("favoriteLimit", DEFAULT_FAVORITE_LIMIT);
    }

    private void updateDatabase() {
        MongoCollection<Document> userCollection = db.getCollection("user");
        userCollection.updateOne(new Document("_id", userId), new Document("$set", userData));
    }

    public Map<String, String> getImage() throws NoEnabledOptionsException, InvalidChancesException {
        // Get the user's image options
        Map<String, ImageOptions> userImageOptions = getAllImageOptions();

        // Get the default image options
        List<ImageOptions> defaultImageOptions = ImageOptions.getDefaultOptions();

        // Create a map to store the final probabilities for each image source
        Map<String, Double> probabilities = new HashMap<>();

        // Calculate the total chance of all enabled options
        double totalChance = 0;
        for (ImageOptions defaultOption : defaultImageOptions) {
            String imageType = defaultOption.getImageType();
            ImageOptions userOption = userImageOptions.get(imageType);
            if (userOption != null && userOption.isEnabled()) {
                totalChance += userOption.getChance();
                probabilities.put(imageType, userOption.getChance());
            } else if (userOption == null && defaultOption.isEnabled()) {
                totalChance += defaultOption.getChance();
                probabilities.put(imageType, defaultOption.getChance());
            }
        }

        // Check if all options are disabled
        if (probabilities.isEmpty()) {
            throw new NoEnabledOptionsException("All image options are disabled");
        }

        // Normalize the probabilities if the total chance exceeds 100
        if (totalChance > 100) {
            double exceededChance = totalChance - 100;
            double minusChance = exceededChance / probabilities.size();
            for (Map.Entry<String, Double> entry : probabilities.entrySet()) {
                double newChance = entry.getValue() - minusChance;
                if (newChance < 0) {
                    throw new InvalidChancesException("Invalid chances: one or more chances are less than 0");
                }
                entry.setValue(newChance);
            }
        }

        // Generate a random number
        double rand = new Random().nextDouble() * 100;

        // Select an image source based on the random number and the probabilities
        double cumulativeProbability = 0;
        for (Map.Entry<String, Double> entry : probabilities.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (rand < cumulativeProbability) {
                String imageType = entry.getKey();
                switch (imageType) {
                    case "4chan":
                        return RandomImage.get4ChanImage(null);
                    case "picsum":
                        return RandomImage.getPicSumImage();
                    case "imgur":
                        return RandomImage.getImgurImage();
                    case "reddit":
                        try {
                            return RandomReddit.getRandomReddit(null);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    case "rule34xxx":
                        return RandomImage.getRandomRule34xxx();
                    case "tenor":
                        try {
                            return RandomImage.getTenor(null);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case "google":
                        try {
                            return RandomImage.getGoogleQueryImage(null);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case "movies":
                        return RandomMedia.randomMovie();
                    case "tvshow":
                        return RandomMedia.randomMovie();
                    default:
                        throw new IllegalArgumentException("Unknown image type: " + imageType);
                }
            }
        }

        // This should never happen (Hopefully...)
        throw new IllegalStateException("Failed to select an image source");
    }

}