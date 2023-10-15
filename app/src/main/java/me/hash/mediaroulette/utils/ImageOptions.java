package me.hash.mediaroulette.utils;

import java.util.ArrayList;
import java.util.List;

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
        List<ImageOptions> options = new ArrayList<>();

        options.add(new ImageOptions("reddit", true, 25));
        options.add(new ImageOptions("imgur", true, 20));
        options.add(new ImageOptions("tenor", true, 15));
        options.add(new ImageOptions("rule34xxx", true, 10));
        options.add(new ImageOptions("4chan", true, 10));
        options.add(new ImageOptions("picsum", true, 5));
        options.add(new ImageOptions("google", true, 15));

        return options;
    }
}

