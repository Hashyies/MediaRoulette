package me.hash.mediaroulette.utils.random;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import me.hash.mediaroulette.Main;

import java.io.IOException;
import java.util.*;

public class RandomMedia {
    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String BASE_IMAGE_URL = "https://image.tmdb.org/t/p/w500";
    private static Map<String, List<Map<String, String>>> cache = new HashMap<>();
    private static final OkHttpClient client = new OkHttpClient();

    public static Map<String, String> randomTVShow() {
        try {
            return getRandomMedia("tv");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, String> randomMovie() {
        try {
            return getRandomMedia("movie");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<String, String> getRandomMedia(String type) throws IOException {
        // Get a random year
        int year = new Random().nextInt(2023 - 1900) + 1900; // Replace 2023 with the current year
    
        // Get a list of media from the /discover endpoint
        Request request = new Request.Builder()
                .url(BASE_URL + "/discover/" + type + "?primary_release_year=" + year + "&api_key=" + Main.getEnv("TMDB_API"))
                .build();
    
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);
    
            // Parse JSON and get list of media
            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONArray results = jsonObject.getJSONArray("results");
            List<Map<String, String>> mediaList = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                Map<String, String> media = parseMedia(item, type);
                mediaList.add(media);
            }
    
            // Cache the result
            cache.put(type, mediaList);
    
            return getRandomFromList(mediaList);
        }
    }
    
    
    private static Map<String, String> parseMedia(JSONObject item, String type) {
        Map<String, String> media = new HashMap<>();
        String title;
        String date;
        if (type.equals("tv")) {
            title = item.getString("original_name"); // Use "original_name" for tv shows
            date = item.getString("first_air_date");
        } else {
            title = item.getString("title"); // Use "title" for movies
            date = item.getString("release_date");
        }

        String posterPath = item.optString("poster_path");
        media.put("image", posterPath.isEmpty() ? "none" : BASE_IMAGE_URL + posterPath);
        
        media.put("description", "🌐 Source: TMDB\n"
                + "✏️ Title: " + title + "\n"
                + "📅 Release Date: " + date + "\n"
                + "⭐ Rating: " + item.getDouble("vote_average") + "/10\n"
                + "🔍 Synopsis: " + item.getString("overview"));

        media.put("title", "Here is your random " + type.replace("tv", "TVShow") + " from TMDB!");

        return media;
    }
    

    private static Map<String, String> getRandomFromList(List<Map<String, String>> list) {
        Random rand = new Random();
        return list.get(rand.nextInt(list.size()));
    }
}
