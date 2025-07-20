package me.hash.mediaroulette.content.provider.impl.videos;

import me.hash.mediaroulette.model.content.MediaResult;
import me.hash.mediaroulette.model.content.MediaSource;
import me.hash.mediaroulette.content.provider.MediaProvider;
import me.hash.mediaroulette.content.http.HttpClientWrapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TMDBTvProvider implements MediaProvider {
    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String BASE_IMAGE_URL = "https://image.tmdb.org/t/p/w500";

    private final Map<Integer, Queue<MediaResult>> yearCache = new ConcurrentHashMap<>();
    private final HttpClientWrapper httpClient;
    private final Random random = new Random();
    private final String apiKey;

    public TMDBTvProvider(HttpClientWrapper httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    @Override
    public MediaResult getRandomMedia(String query) throws IOException, HttpClientWrapper.RateLimitException, InterruptedException {
        int year = random.nextInt(2023 - 1900) + 1900;
        Queue<MediaResult> cache = yearCache.computeIfAbsent(year, k -> new LinkedList<>());

        if (cache.isEmpty()) {
            populateCache(year);
        }

        MediaResult result = cache.poll();
        if (result == null) {
            throw new IOException("No TV shows available for year: " + year);
        }
        return result;
    }

    private void populateCache(int year) throws IOException, HttpClientWrapper.RateLimitException, InterruptedException {
        String url = String.format("%s/discover/tv?primary_release_year=%d&api_key=%s",
                BASE_URL, year, apiKey);

        String response = httpClient.getBody(url);
        JSONObject jsonObject = new JSONObject(response);
        JSONArray results = jsonObject.getJSONArray("results");

        List<MediaResult> tvShows = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);
            tvShows.add(parseMedia(item));
        }

        Queue<MediaResult> cache = yearCache.get(year);
        cache.addAll(tvShows);
    }

    private MediaResult parseMedia(JSONObject item) {
        String title = item.getString("original_name");
        String date = item.getString("first_air_date");
        String posterPath = item.optString("poster_path");
        String imageUrl = posterPath.isEmpty() ? "none" : BASE_IMAGE_URL + posterPath;

        String description = String.format("🌐 Source: TMDB\n✏️ Title: %s\n📅 Release Date: %s\n⭐ Rating: %.1f/10\n🔍 Synopsis: %s",
                title, date, item.getDouble("vote_average"), item.getString("overview"));
        String resultTitle = "Here is your random TVShow from TMDB!";

        return new MediaResult(imageUrl, resultTitle, description, MediaSource.TMDB);
    }

    @Override
    public boolean supportsQuery() {
        return false; // Uses random year selection instead of query
    }

    @Override
    public String getProviderName() {
        return "TMDB TV Shows";
    }
}