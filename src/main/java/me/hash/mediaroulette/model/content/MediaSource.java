// MediaSource.java - Enum for different sources
package me.hash.mediaroulette.model.content;

public enum MediaSource {
    CHAN_4("4Chan"),
    PICSUM("Picsum"),
    IMGUR("Imgur"),
    RULE34("Rule34"),
    GOOGLE("Google"),
    TENOR("Tenor"),
    REDDIT("Reddit"),
    TMDB("TMDB"),
    YOUTUBE("Youtube");

    private final String displayName;

    MediaSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}