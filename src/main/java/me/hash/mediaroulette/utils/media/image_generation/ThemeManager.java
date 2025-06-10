package me.hash.mediaroulette.utils.media.image_generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ThemeManager {
    private static final Logger LOGGER = Logger.getLogger(ThemeManager.class.getName());
    private static ThemeManager instance;
    private Map<String, Theme> themes;
    private ObjectMapper objectMapper;

    private ThemeManager() {
        this.objectMapper = new ObjectMapper();
        this.themes = new HashMap<>();
        loadThemes();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager();
                }
            }
        }
        return instance;
    }

    private void loadThemes() {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("config/themes.json")) {

            if (inputStream == null) {
                LOGGER.severe("themes.json not found in resources/config/");
                throw new IOException("themes.json not found in resources/config/");
            }

            TypeFactory typeFactory = objectMapper.getTypeFactory();
            List<Theme> themeList = objectMapper.readValue(inputStream,
                    typeFactory.constructCollectionType(List.class, Theme.class));

            // Clear existing themes and load from config
            themes.clear();
            for (Theme theme : themeList) {
                if (theme.getName() != null && !theme.getName().trim().isEmpty()) {
                    themes.put(theme.getName(), theme);
                    LOGGER.info("Loaded theme: " + theme.getName());
                } else {
                    LOGGER.warning("Skipped theme with null or empty name");
                }
            }

            LOGGER.info("Successfully loaded " + themes.size() + " themes from config");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load themes from config file", e);
            // If config loading fails, we'll have an empty themes map
            // The getTheme method will handle this gracefully
        }
    }

    /**
     * Gets a theme by name. If the theme doesn't exist, returns null.
     * It's the caller's responsibility to handle null themes appropriately.
     */
    public Theme getTheme(String themeName) {
        if (themeName == null || themeName.trim().isEmpty()) {
            LOGGER.warning("Attempted to get theme with null or empty name");
            return getDefaultTheme();
        }

        Theme theme = themes.get(themeName);
        if (theme == null) {
            LOGGER.warning("Theme '" + themeName + "' not found, returning default theme");
            return getDefaultTheme();
        }

        return theme;
    }

    /**
     * Returns the first available theme as a fallback, or null if no themes are loaded
     */
    private Theme getDefaultTheme() {
        if (themes.isEmpty()) {
            LOGGER.severe("No themes available! Please check your themes.json configuration.");
            return null;
        }

        // Return the first theme as default
        return themes.values().iterator().next();
    }

    /**
     * Gets all available theme names
     */
    public java.util.Set<String> getAvailableThemeNames() {
        return new java.util.HashSet<>(themes.keySet());
    }

    /**
     * Returns a copy of all themes
     */
    public Map<String, Theme> getAllThemes() {
        return new HashMap<>(themes);
    }

    /**
     * Checks if a theme exists
     */
    public boolean hasTheme(String themeName) {
        return themeName != null && themes.containsKey(themeName);
    }

    /**
     * Gets the number of loaded themes
     */
    public int getThemeCount() {
        return themes.size();
    }

    /**
     * Reloads themes from the config file
     */
    public void reloadThemes() {
        LOGGER.info("Reloading themes from config file");
        loadThemes();
    }
}