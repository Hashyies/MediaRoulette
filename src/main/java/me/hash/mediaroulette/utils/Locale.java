package me.hash.mediaroulette.utils;

import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.InputStream;

public class Locale {
    private final JSONObject translations;

    /**
     * Loads the JSON locale file from resources/locales.
     *
     * @param localeName the locale identifier (e.g., "en_US")
     */
    public Locale(String localeName) {
        String path = "/locales/" + localeName + ".json";
        InputStream in = getClass().getResourceAsStream(path);
        if (in == null) {
            throw new RuntimeException("Locale file not found: " + path);
        }
        translations = new JSONObject(new JSONTokener(in));
    }

    /**
     * Retrieves the translation for the given key.
     * If the key is not found, the key itself is returned.
     *
     * @param key the translation key
     * @return the corresponding translation or the key if not found
     */
    public String get(String key) {
        return translations.optString(key, key);
    }
}
