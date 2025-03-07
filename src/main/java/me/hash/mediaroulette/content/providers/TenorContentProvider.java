package me.hash.mediaroulette.content.providers;

import me.hash.mediaroulette.content.ContentInfo;
import me.hash.mediaroulette.content.ContentProvider;
import me.hash.mediaroulette.Main;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Random;

public class TenorContentProvider implements ContentProvider {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final Random RANDOM = new Random();

    @Override
    public ContentInfo getRandomContent() throws IOException {
        String query = "random"; // You can parameterize this as needed.
        String apiKey = Main.getEnv("TENOR_API");
        String url = "https://tenor.googleapis.com/v2/search?key=" + apiKey +
                "&q=" + URLEncoder.encode(query, "UTF-8") +
                "&limit=50";

        Request request = new Request.Builder().url(url).build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        JSONObject jsonObject = new JSONObject(response.body().string());
        JSONArray resultsArray = jsonObject.getJSONArray("results");
        int randomIndex = RANDOM.nextInt(resultsArray.length());
        JSONObject resultObject = resultsArray.getJSONObject(randomIndex);

        if (resultObject.has("media_formats") &&
                resultObject.getJSONObject("media_formats").has("gif")) {
            String gifUrl = resultObject.getJSONObject("media_formats")
                    .getJSONObject("gif").getString("url");
            String title = "Here is your random Tenor image!";
            String description = String.format("🌐 Source: Tenor\n🔎 Query: %s", query);
            return new ContentInfo(title, description, gifUrl);
        } else {
            throw new IOException("Expected media format 'gif' not found");
        }
    }
}
