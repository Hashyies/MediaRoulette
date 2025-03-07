package me.hash.mediaroulette.content.providers;

import me.hash.mediaroulette.content.ContentInfo;
import me.hash.mediaroulette.content.ContentProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;

public class Rule34ContentProvider implements ContentProvider {
    @Override
    public ContentInfo getRandomContent() throws IOException {
        String url = "https://rule34.xxx/index.php?page=post&s=random";
        String imageUrl = null;
        try {
            Document doc = Jsoup.connect(url).get();
            Elements image = doc.select("#image");
            if (!image.isEmpty()) {
                imageUrl = image.attr("src");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String title = "Here is your random Rule34xxx (NSFW) picture!";
        String description = "🌐 Source: Rule34";
        return new ContentInfo(title, description, imageUrl);
    }
}
