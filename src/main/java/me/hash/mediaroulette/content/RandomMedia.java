package me.hash.mediaroulette.content;

import me.hash.mediaroulette.utils.discord.DiscordTimestamp;
import me.hash.mediaroulette.utils.discord.DiscordTimestampType;
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
    private static final OkHttpClient client = new OkHttpClient();
    private static final Random random = new Random();

    public static Map<String, String> randomTVShow() throws IOException {
        return getRandomMedia("tv");
    }

    public static Map<String, String> randomMovie() throws IOException {
        return getRandomMedia("movie");
    }

    private static Map<String, String> getRandomMedia(String type) throws IOException {
        int year = random.nextInt(2023 - 1900) + 1900;
        Request request = new Request.Builder()
                .url(BASE_URL + "/discover/" + type + "?primary_release_year=" + year + "&api_key="
                        + Main.getEnv("TMDB_API"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);

            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONArray results = jsonObject.getJSONArray("results");
            List<Map<String, String>> mediaList = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                mediaList.add(parseMedia(item, type));
            }

            return mediaList.get(random.nextInt(mediaList.size()));
        }
    }

    private static Map<String, String> parseMedia(JSONObject item, String type) {
        Map<String, String> media = new HashMap<>();
        String title = type.equals("tv") ? item.getString("original_name") : item.getString("title");
        String date = type.equals("tv") ? item.getString("first_air_date") : item.getString("release_date");
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

    public static HashMap<String, String> getRandomYoutubeShorts() throws IOException {
        return getRandomYoutubeVideo("shorts");
    }

    public static HashMap<String, String> getRandomYoutube() throws IOException {
        return getRandomYoutubeVideo("");
    }

    private static HashMap<String, String> getRandomYoutubeVideo(String videoType) throws IOException {
        String[] filters = { "music", "sports", "gaming", "movies", "news", "live", "learning" };
        String[] orders = { "date", "rating", "relevance", "title", "viewCount" };
        int randomFilterIndex = random.nextInt(filters.length);
        int randomOrderIndex = random.nextInt(orders.length);

        String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=25&key="
                + Main.getEnv("GOOGLE_API_KEY");

        if (videoType.equals("shorts")) {
            url += "&videoDuration=short&q=%23shorts" + "&order=" + orders[randomOrderIndex];
        } else {
            url += "&topicId=" + filters[randomFilterIndex] + "&order=" + orders[randomOrderIndex];
        }

        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        String jsonData = response.body().string();

        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray itemsArray = jsonObject.getJSONArray("items");

        int randomVideoIndex = random.nextInt(itemsArray.length());

        JSONObject randomVideo = itemsArray.getJSONObject(randomVideoIndex);
        JSONObject snippet = randomVideo.getJSONObject("snippet");
        JSONObject id = randomVideo.getJSONObject("id");

        String title = snippet.getString("title");
        String channelTitle = snippet.getString("channelTitle");
        String publishDate = snippet.getString("publishedAt");
        String thumbnailUrl = snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url");
        String videoId = id.getString("videoId");
        String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

        HashMap<String, String> videoDetails = new HashMap<>();
        videoDetails.put("description", "🎬 **Title:** " + title
                + "\n📺 **Channel Name:** " + channelTitle
                + "\n📅 **Date Of Release:** " + DiscordTimestamp.generateTimestampFromIso8601(publishDate, DiscordTimestampType.SHORT_DATE_TIME)
                + "\n🔗 **Video Link:** " + "<" + videoUrl + ">");

        videoDetails.put("image", thumbnailUrl);
        videoDetails.put("link", videoUrl);
        videoDetails.put("title",
                "Here is your random " + (videoType.equals("shorts") ? "short " : "") + "YouTube video!");

        return videoDetails;
    }
}
