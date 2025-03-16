package me.hash.mediaroulette.model;

public class Favorite {
    private int id;
    private String description;
    private String image;
    private String type;

    public Favorite(int id, String description, String image, String type) {
        this.id = id;
        this.description = description;
        this.image = image;
        this.type = type;
    }

    // --- Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
