package me.hash.mediaroulette.model;

import me.hash.mediaroulette.exceptions.InvalidChancesException;
import me.hash.mediaroulette.exceptions.NoEnabledOptionsException;
import me.hash.mediaroulette.utils.user.ImageSelector;

import java.util.*;

public class User {
    public static final int DEFAULT_FAVORITE_LIMIT = 25;

    private String userId;
    private long imagesGenerated;
    private boolean nsfw;
    private boolean premium;
    private boolean admin;
    private List<Favorite> favorites;
    private Map<String, ImageOptions> imageOptions;
    private String locale; // locale support
    private String theme;

    public User(String userId) {
        this.userId = userId;
        this.imagesGenerated = 0;
        this.nsfw = false;
        this.premium = false;
        this.admin = false;
        this.favorites = new ArrayList<>();
        this.imageOptions = new HashMap<>();
        this.locale = "en_US"; // default locale
    }

    // --- Getters and Setters ---
    public String getUserId() { return userId; }
    public long getImagesGenerated() { return imagesGenerated; }
    public void setImagesGenerated(long imagesGenerated) { this.imagesGenerated = imagesGenerated; }
    public boolean isNsfw() { return nsfw; }
    public void setNsfw(boolean nsfw) { this.nsfw = nsfw; }
    public boolean isPremium() { return premium; }
    public void setPremium(boolean premium) { this.premium = premium; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    public List<Favorite> getFavorites() { return favorites; }
    public Map<String, ImageOptions> getImageOptionsMap() { return imageOptions; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    // --- Business Logic Methods ---
    public void incrementImagesGenerated() {
        this.imagesGenerated++;
    }

    public int getFavoriteLimit() {
        return premium ? DEFAULT_FAVORITE_LIMIT * 2 : DEFAULT_FAVORITE_LIMIT;
    }

    public void addFavorite(String description, String image, String type) {
        if (favorites.size() >= getFavoriteLimit()) {
            // Log warning as needed – favorite limit reached.
            return;
        }
        int id = favorites.size();
        favorites.add(new Favorite(id, description, image, type));
    }

    public void removeFavorite(int id) {
        if (id < 0 || id >= favorites.size()) return;
        favorites.remove(id);
        // Reassign IDs so they remain sequential.
        for (int i = id; i < favorites.size(); i++) {
            favorites.get(i).setId(i);
        }
    }

    /**
     * Update (or set) image options with new chances.
     */
    public void setChances(ImageOptions... options) {
        for (ImageOptions option : options) {
            imageOptions.put(option.getImageType(), option);
        }
    }

    public ImageOptions getImageOptions(String imageType) {
        return imageOptions.get(imageType);
    }

    /**
     * Uses the ImageSelector to pick an image option.
     * @return a map containing the selected image details.
     * @throws NoEnabledOptionsException if no enabled options exist.
     * @throws InvalidChancesException if the chance values are invalid.
     */
    public Map<String, String> getImage() throws NoEnabledOptionsException, InvalidChancesException, me.hash.mediaroulette.exceptions.InvalidChancesException, me.hash.mediaroulette.exceptions.NoEnabledOptionsException {
        ImageSelector selector = new ImageSelector(imageOptions);
        return selector.selectImage();
    }
}
