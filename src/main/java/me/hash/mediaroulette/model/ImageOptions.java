package me.hash.mediaroulette.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ImageOptions {
    private final String imageType;
    private boolean enabled;
    private double chance;

    public ImageOptions(String imageType, boolean enabled, double chance) {
        this.imageType = imageType;
        this.enabled = enabled;
        this.chance = chance;
    }

    public String getImageType() {
        return imageType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public static List<ImageOptions> getDefaultOptions() {
        InputStream is = ImageOptions.class.getResourceAsStream("/config/randomWeightValues.json");
        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            String json = scanner.useDelimiter("\\A").next();
            JSONArray array = new JSONArray(json);
            List<ImageOptions> options = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String imageType = object.getString("imageType");
                boolean enabled = object.getBoolean("enabled");
                double chance = object.getDouble("chance");
                options.add(new ImageOptions(imageType, enabled, chance));
            }
            return options;
        }
    }
}