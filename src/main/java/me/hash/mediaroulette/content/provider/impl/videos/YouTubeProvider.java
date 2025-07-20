package me.hash.mediaroulette.content.provider.impl.videos;

import me.hash.mediaroulette.model.content.MediaResult;
import me.hash.mediaroulette.model.content.MediaSource;
import me.hash.mediaroulette.content.provider.MediaProvider;
import me.hash.mediaroulette.content.http.HttpClientWrapper;
import me.hash.mediaroulette.utils.discord.DiscordTimestamp;
import me.hash.mediaroulette.utils.discord.DiscordTimestampType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class YouTubeProvider implements MediaProvider {
    private static final String[] FILTERS = {"music", "sports", "gaming", "movies", "news", "live", "learning"};
    private static final String[] ORDERS = {"date", "rating", "relevance", "title", "viewCount"};

    private final Map<String, Queue<MediaResult>> topicCache = new ConcurrentHashMap<>();
    private final HttpClientWrapper httpClient;
    private final Random random = new Random();
    private final String apiKey;

    public YouTubeProvider(HttpClientWrapper httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    @Override
    public MediaResult getRandomMedia(String query) throws IOException, HttpClientWrapper.RateLimitException, InterruptedException {
        String topic = FILTERS[random.nextInt(FILTERS.length)];
        String order = ORDERS[random.nextInt(ORDERS.length)];
        String cacheKey = topic + "_" + order;

        Queue<MediaResult> cache = topicCache.computeIfAbsent(cacheKey, k -> new LinkedList<>());

        if (cache.isEmpty()) {
            populateCache(topic, order);
        }

        MediaResult result = cache.poll();
        if (result == null) {
            throw new IOException("No YouTube videos available for topic: " + topic);
        }
        return result;
    }

    private void populateCache(String topic, String order) throws IOException, HttpClientWrapper.RateLimitException, InterruptedException {
        String url = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=25&key=%s&topicId=%s&order=%s",
                apiKey, topic, order);

        String response = httpClient.getBody(url);
        JSONObject jsonObject = new JSONObject(response);
        JSONArray itemsArray = jsonObject.getJSONArray("items");

        List<MediaResult> videos = new ArrayList<>();
        for (int i = 0; i < itemsArray.length(); i++) {
            JSONObject video = itemsArray.getJSONObject(i);
            videos.add(parseVideo(video));
        }

        String cacheKey = topic + "_" + order;
        Queue<MediaResult> cache = topicCache.get(cacheKey);
        cache.addAll(videos);
    }

    private MediaResult parseVideo(JSONObject video) {
        JSONObject snippet = video.getJSONObject("snippet");
        JSONObject id = video.getJSONObject("id");

        String title = snippet.getString("title");
        String channelTitle = snippet.getString("channelTitle");
        String publishDate = snippet.getString("publishedAt");
        String thumbnailUrl = snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url");
        String videoId = id.getString("videoId");
        String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

        String description = String.format("🎬 **Title:** %s\n📺 **Channel Name:** %s\n📅 **Date Of Release:** %s\n🔗 **Video Link:** <%s>",
                title, channelTitle,
                DiscordTimestamp.generateTimestampFromIso8601(publishDate, DiscordTimestampType.SHORT_DATE_TIME),
                videoUrl);
        String resultTitle = "Here is your random YouTube video!";

        return new MediaResult(thumbnailUrl, resultTitle, description, MediaSource.YOUTUBE);
    }

    @Override
    public boolean supportsQuery() {
        return false; // Uses random topic selection instead of query
    }

    @Override
    public String getProviderName() {
        return "YouTube";
    }
}