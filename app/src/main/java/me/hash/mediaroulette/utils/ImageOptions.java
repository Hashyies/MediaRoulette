package me.hash.mediaroulette.utils;

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
}

