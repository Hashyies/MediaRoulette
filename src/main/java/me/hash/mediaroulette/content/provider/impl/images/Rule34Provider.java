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
    public MediaResult getRandomMedia(String query) throws IOException {
        String url = "https://rule34.xxx/index.php?page=post&s=random";
        String response = httpClient.get(url);

        Document doc = Jsoup.parse(response);
        Elements image = doc.select("#image");

        if (image.isEmpty()) {
            throw new IOException("No image found on Rule34xxx");
        }

        String imageUrl = image.attr("src");
        if (imageUrl.isEmpty()) {
            throw new IOException("Invalid image URL from Rule34xxx");
        }

        String description = "🌐 Source: Rule34";
        String title = "Here is your random Rule34xxx (NSFW) picture!";

        return new MediaResult(imageUrl, title, description, MediaSource.RULE34);
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