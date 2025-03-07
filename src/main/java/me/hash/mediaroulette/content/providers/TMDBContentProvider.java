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
import java.util.*;

public class TMDBContentProvider implements ContentProvider {
    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String BASE_IMAGE_URL = "https://image.tmdb.org/t/p/w500";
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final Random RANDOM = new Random();
    private final String type; // "movie" or "tv"

    public TMDBContentProvider(String type) {
        if (!type.equals("movie") && !type.equals("tv")) {
            throw new IllegalArgumentException("Type must be 'movie' or 'tv'");
        }
        this.type = type;
    }

    @Override
    public ContentInfo getRandomContent() throws IOException {
        // Pick a random year between 1900 and 2022.
        int year = RANDOM.nextInt(2023 - 1900) + 1900;
        String url = BASE_URL + "/discover/" + type + "?primary_release_year=" + year + "&api_key=" + Main.getEnv("TMDB_API");
        Request request = new Request.Builder().url(url).build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);
            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONArray results = jsonObject.getJSONArray("results");
            List<JSONObject> mediaList = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                mediaList.add(results.getJSONObject(i));
            }
            JSONObject item = mediaList.get(RANDOM.nextInt(mediaList.size()));
            return parseMedia(item);
        }
    }

    private ContentInfo parseMedia(JSONObject item) {
        String title = type.equals("tv") ? item.getString("original_name") : item.getString("title");
        String date = type.equals("tv") ? item.getString("first_air_date") : item.getString("release_date");
        String posterPath = item.optString("poster_path");
        String image = posterPath.isEmpty() ? "none" : BASE_IMAGE_URL + posterPath;
        String description = "🌐 Source: TMDB\n"
                + "✏️ Title: " + title + "\n"
                + "📅 Release Date: " + date + "\n"
                + "⭐ Rating: " + item.getDouble("vote_average") + "/10\n"
                + "🔍 Synopsis: " + item.getString("overview");
        String providerTitle = "Here is your random " + (type.equals("tv") ? "TV Show" : "Movie") + " from TMDB!";
        return new ContentInfo(providerTitle, description, image);
    }
}
