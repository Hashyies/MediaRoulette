package me.hash.mediaroulette.content;

public class ContentInfo {
    private String title;
    private String description;
    private String imageUrl;
    private String link; // optional, for example for YouTube videos

    public ContentInfo(String title, String description, String imageUrl) {
        this(title, description, imageUrl, null);
    }

    public ContentInfo(String title, String description, String imageUrl, String link) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.link = link;
    }

    // Getters
    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public String getLink() {
        return link;
    }
}
