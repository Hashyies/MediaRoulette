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
            String apiUrl = "https://rule34.xxx/index.php?page=post&s=random";

            String imageUrl = null;
            try {
                Document doc = Jsoup.connect(apiUrl).get();
                Elements image = doc.select("#image");
                if (image.size() != 0) {
                    imageUrl = image.attr("src");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String description = "Source: Rule34 - NSFW Content";
            String title = "Here is your random Rule34xxx (NSFW) picture!";

            return new MediaResult(imageUrl, title, description, MediaSource.RULE34);
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