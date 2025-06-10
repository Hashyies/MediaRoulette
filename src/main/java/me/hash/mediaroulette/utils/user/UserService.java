package me.hash.mediaroulette.utils.user;

import me.hash.mediaroulette.model.ImageOptions;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.repository.UserRepository;

import java.util.concurrent.ConcurrentHashMap;

public class UserService {
    private final UserRepository userRepository;
    // Simple in-memory cache; replace with a more robust solution if needed.
    private final ConcurrentHashMap<String, User> cache = new ConcurrentHashMap<>();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Fetches an existing user or creates a new one if not found.
     */
    public User getOrCreateUser(String userId) {
        return cache.computeIfAbsent(userId, id -> {
            return userRepository.findById(id)
                    .orElseGet(() -> {
                        User newUser = new User(id);
                        return userRepository.save(newUser);
                    });
        });
    }

    /**
     * Checks if a user exists.
     */
    public boolean userExists(String userId) {
        return cache.containsKey(userId) || userRepository.exists(userId);
    }

    /**
     * Update the user's locale setting.
     */
    public void updateLocale(String userId, String locale) {
        User user = getOrCreateUser(userId);
        user.setLocale(locale);
        updateUser(user);
    }

    /**
     * Generic update method for a user.
     */
    public void updateUser(User user) {
        userRepository.save(user);
        cache.put(user.getUserId(), user);
    }

    // --- Convenience Methods for Updating User Properties ---

    public void incrementImagesGenerated(String userId) {
        User user = getOrCreateUser(userId);
        user.incrementImagesGenerated();
        updateUser(user);
    }

    public void setNsfwEnabled(String userId, boolean enabled) {
        User user = getOrCreateUser(userId);
        user.setNsfw(enabled);
        updateUser(user);
    }

    public void setPremium(String userId, boolean premium) {
        User user = getOrCreateUser(userId);
        user.setPremium(premium);
        updateUser(user);
    }

    public void setAdmin(String userId, boolean admin) {
        User user = getOrCreateUser(userId);
        user.setAdmin(admin);
        updateUser(user);
    }

    public void addFavorite(String userId, String description, String image, String type) {
        User user = getOrCreateUser(userId);
        user.addFavorite(description, image, type);
        updateUser(user);
    }

    public void removeFavorite(String userId, int favoriteId) {
        User user = getOrCreateUser(userId);
        user.removeFavorite(favoriteId);
        updateUser(user);
    }

    public void setChances(String userId, ImageOptions... options) {
        User user = getOrCreateUser(userId);
        user.setChances(options);
        updateUser(user);
    }
}
