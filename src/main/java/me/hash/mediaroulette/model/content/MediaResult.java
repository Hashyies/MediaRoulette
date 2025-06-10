package me.hash.mediaroulette.model.content;

import java.util.HashMap;
import java.util.Map;

public class MediaResult {
    private final String imageUrl;
    private final String title;
    private final String description;
    private final MediaSource source;
    private final String imageType;
    private final String imageContent;

    public MediaResult(String imageUrl, String title, String description, MediaSource source) {
        this(imageUrl, title, description, source, null, null);
    }

    public MediaResult(String imageUrl, String title, String description, MediaSource source, String imageType, String imageContent) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.description = description;
        this.source = source;
        this.imageType = imageType;
        this.imageContent = imageContent;
    }

    // Getters
    public String getImageUrl() { return imageUrl; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public MediaSource getSource() { return source; }
    public String getImageType() { return imageType; }
    public String getImageContent() { return imageContent; }

    // For backward compatibility - returns Map format
    public Map<String, String> toMap() {
        Map<String, String> result = new HashMap<>();
        result.put("image", imageUrl);
        result.put("title", title);
        result.put("description", description);
        if (imageType != null) {
            result.put("image_type", imageType);
        }
        if (imageContent != null) {
            result.put("image_content", imageContent);
        }
        return result;
    }
}