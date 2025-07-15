// PicsumProvider.java
package me.hash.mediaroulette.content.provider.impl.images;

import me.hash.mediaroulette.model.content.MediaResult;
import me.hash.mediaroulette.model.content.MediaSource;
import me.hash.mediaroulette.content.provider.MediaProvider;
import me.hash.mediaroulette.content.http.HttpClientWrapper;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class PicsumProvider implements MediaProvider {
    private final HttpClientWrapper httpClient;

    public PicsumProvider(HttpClientWrapper httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public MediaResult getRandomMedia(String query) throws IOException {
        // Try different approaches to get a random Picsum image
        String imageUrl = getRandomPicsumImage();

        return new MediaResult(
                imageUrl,
                "Here is your random Picsum image!",
                "🌐 Source: Picsum\n📏 Resolution: 1920x1080\n🎲 Random ID: " + extractImageId(imageUrl),
                MediaSource.PICSUM
        );
    }

    private String getRandomPicsumImage() throws IOException {
        try {
            return getRandomImageDirect();
        } catch (IOException e) {
            System.err.println("Method 1 failed: " + e.getMessage());
        }

        try {
            return getRandomImageById();
        } catch (IOException e) {
            System.err.println("Method 2 failed: " + e.getMessage());
        }

        // Method 3: Fallback to a specific image
        return getFallbackImage();
    }

    private String getRandomImageById() throws IOException {
        // Generate a random image ID (Picsum has images from 1 to ~1000+)
        int randomId = (int) (Math.random() * 1000) + 1;
        String url = "https://picsum.photos/id/" + randomId + "/1920/1080";
        
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        try (Response response = httpClient.getClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                return url;
            } else {
                throw new IOException("Failed to get image with ID " + randomId + ": " + response.code());
            }
        }
    }

    private String getRandomImageDirect() throws IOException {
        String url = "https://picsum.photos/1920/1080";
        
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        try (Response response = httpClient.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Picsum API returned error: " + response.code());
            }

            // Get the final URL after any redirects
            String finalUrl = response.request().url().toString();
            
            // If we're still at the original URL, check for redirect header
            if (finalUrl.equals(url)) {
                String redirectUrl = response.header("Location");
                if (redirectUrl != null) {
                    return redirectUrl;
                }
                // If no redirect, the original URL should work
                return url;
            }
            
            return finalUrl;
        }
    }

    private String getFallbackImage() {
        // Fallback to a known working image
        return "https://picsum.photos/id/1/1920/1080";
    }

    private String extractImageId(String imageUrl) {
        try {
            if (imageUrl.contains("/id/")) {
                String[] parts = imageUrl.split("/id/")[1].split("/");
                return parts[0];
            }
            return "Random";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public boolean supportsQuery() {
        return false;
    }

    @Override
    public String getProviderName() {
        return "Picsum";
    }
}