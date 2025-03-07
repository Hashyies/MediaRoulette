package me.hash.mediaroulette.content.providers;

import me.hash.mediaroulette.content.ContentInfo;
import me.hash.mediaroulette.content.ContentProvider;
import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.utils.discord.DiscordTimestamp;
import me.hash.mediaroulette.utils.discord.DiscordTimestampType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

public class YouTubeContentProvider implements ContentProvider {
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final Random RANDOM = new Random();
    private final String videoType; // "" for normal videos, "shorts" for YouTube Shorts

    public YouTubeContentProvider(String videoType) {
        this.videoType = videoType;
    }

    @Override
    public ContentInfo getRandomContent() throws IOException {
        String[] filters = { "music", "sports", "gaming", "movies", "news", "live", "learning" };
        String[] orders = { "date", "rating", "relevance", "title", "viewCount" };
        int randomFilterIndex = RANDOM.nextInt(filters.length);
        int randomOrderIndex = RANDOM.nextInt(orders.length);

        String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=25&key="
                + Main.getEnv("GOOGLE_API_KEY");

        if (videoType.equals("shorts")) {
            url += "&videoDuration=short&q=%23shorts" + "&order=" + orders[randomOrderIndex];
        } else {
            url += "&topicId=" + filters[randomFilterIndex] + "&order=" + orders[randomOrderIndex];
        }

        Request request = new Request.Builder().url(url).build();
        Response response = CLIENT.newCall(request).execute();
        String jsonData = response.body().string();

        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray itemsArray = jsonObject.getJSONArray("items");
        int randomVideoIndex = RANDOM.nextInt(itemsArray.length());
        JSONObject randomVideo = itemsArray.getJSONObject(randomVideoIndex);
        JSONObject snippet = randomVideo.getJSONObject("snippet");
        JSONObject id = randomVideo.getJSONObject("id");

        String title = snippet.getString("title");
        String channelTitle = snippet.getString("channelTitle");
        String publishDate = snippet.getString("publishedAt");
        String thumbnailUrl = snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url");
        String videoId = id.getString("videoId");
        String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

        String description = "🎬 **Title:** " + title
                + "\n📺 **Channel Name:** " + channelTitle
                + "\n📅 **Date Of Release:** " + DiscordTimestamp.generateTimestampFromIso8601(publishDate, DiscordTimestampType.SHORT_DATE_TIME)
                + "\n🔗 **Video Link:** " + "<" + videoUrl + ">";
        String providerTitle = "Here is your random " + (videoType.equals("shorts") ? "short " : "") + "YouTube video!";

        // Note: we store the video URL in the optional 'link' field.
        return new ContentInfo(providerTitle, description, thumbnailUrl, videoUrl);
    }
}
