package me.hash.mediaroulette.content.provider.impl.images;

import me.hash.mediaroulette.model.content.MediaResult;
import me.hash.mediaroulette.model.content.MediaSource;
import me.hash.mediaroulette.content.provider.MediaProvider;
import me.hash.mediaroulette.content.http.HttpClientWrapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class Rule34Provider implements MediaProvider {
    private final HttpClientWrapper httpClient;

    public Rule34Provider(HttpClientWrapper httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public MediaResult getRandomMedia(String query) throws IOException, HttpClientWrapper.RateLimitException, InterruptedException {
        try {
            // Use API endpoint instead of web scraping to avoid 403 errors
            String apiUrl = "https://api.rule34.xxx/index.php?page=dapi&s=post&q=index&limit=1&pid=" + 
                           (int)(Math.random() * 1000);
            
            String response = httpClient.getBody(apiUrl);
            Document doc = Jsoup.parse(response);
            Elements posts = doc.select("post");

            if (posts.isEmpty()) {
                // Try a different approach with a simpler API call
                apiUrl = "https://api.rule34.xxx/index.php?page=dapi&s=post&q=index&limit=1";
                response = httpClient.getBody(apiUrl);
                doc = Jsoup.parse(response);
                posts = doc.select("post");
                
                if (posts.isEmpty()) {
                    throw new IOException("No posts found on Rule34 API");
                }
            }

            String imageUrl = posts.first().attr("file_url");
            if (imageUrl.isEmpty()) {
                imageUrl = posts.first().attr("sample_url");
            }
            
            if (imageUrl.isEmpty()) {
                throw new IOException("No image URL found in Rule34 response");
            }

            // Ensure the URL is absolute
            if (imageUrl.startsWith("//")) {
                imageUrl = "https:" + imageUrl;
            } else if (imageUrl.startsWith("/")) {
                imageUrl = "https://rule34.xxx" + imageUrl;
            }

            String description = "Source: Rule34 - NSFW Content";
            String title = "Here is your random Rule34xxx (NSFW) picture!";

            return new MediaResult(imageUrl, title, description, MediaSource.RULE34);
        } catch (HttpClientWrapper.RateLimitException e) {
            throw e; // Re-throw rate limit exceptions
        } catch (Exception e) {
            throw new IOException("Failed to get Rule34 image: " + e.getMessage());
        }
    }

    @Override
    public boolean supportsQuery() {
        return false; // Rule34xxx random endpoint doesn't support queries
    }

    @Override
    public String getProviderName() {
        return "Rule34xxx";
    }
}