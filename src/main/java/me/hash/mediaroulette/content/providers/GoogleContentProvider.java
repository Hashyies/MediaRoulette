package me.hash.mediaroulette.content.providers;

import me.hash.mediaroulette.content.ContentInfo;
import me.hash.mediaroulette.content.ContentProvider;
import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.utils.ImageUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

public class GoogleContentProvider implements ContentProvider {
    private static final Random RANDOM = new Random();
    private static final Map<String, List<Map<String, String>>> CACHE = new HashMap<>();

    @Override
    public ContentInfo getRandomContent() throws IOException {
        String query = "random"; // You can parameterize this if desired.
        if (!CACHE.containsKey(query) || CACHE.get(query).isEmpty()) {
            String apiKey = Main.getEnv("GOOGLE_API_KEY");
            String cseId = Main.getEnv("GOOGLE_CX");
            int start = RANDOM.nextInt(5) + 1;
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = String.format(
                    "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&searchType=image&start=%d",
                    apiKey, cseId, encodedQuery, start);
            String response = ImageUtils.httpGet(url);
            JSONObject json = new JSONObject(response);
            JSONArray items = json.getJSONArray("items");

            List<Map<String, String>> images = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                Map<String, String> imageInfo = new HashMap<>();
                imageInfo.put("image", item.getString("link"));
                imageInfo.put("description", String.format("🌐 Source: Google\n🔎 Query: %s\n✏️ Title: %s",
                        query, item.getString("snippet")));
                imageInfo.put("title", "Here is your random Google search image!");
                images.add(imageInfo);
            }
            CACHE.put(query, images);
        }

        List<Map<String, String>> images = CACHE.get(query);
        Map<String, String> data = images.remove(RANDOM.nextInt(images.size()));
        return new ContentInfo("Random Google Image", data.get("description"), data.get("image"));
    }
}
