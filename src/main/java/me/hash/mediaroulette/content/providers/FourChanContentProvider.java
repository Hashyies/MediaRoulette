package me.hash.mediaroulette.content.providers;

import me.hash.mediaroulette.content.ContentInfo;
import me.hash.mediaroulette.content.ContentProvider;
import me.hash.mediaroulette.utils.ImageUtils;
import okhttp3.OkHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FourChanContentProvider implements ContentProvider {
    private static final List<String> BOARDS = Arrays.asList(
            "a", "c", "w", "m", "cgl", "cm", "n", "jp", "vp", "v", "vg",
            "vr", "co", "g", "tv", "k", "o", "an", "tg", "sp", "asp", "sci",
            "int", "out", "toy", "biz", "i", "po", "p", "ck", "ic", "wg", "mu",
            "fa", "3", "gd", "diy", "wsg", "s", "hc", "hm", "h", "e", "u", "d",
            "y", "t", "hr", "gif", "trv", "fit", "x", "lit", "adv", "lgbt", "mlp",
            "b", "r", "r9k", "pol", "soc", "s4s"
    );
    private static final Random RANDOM = new Random();
    private static final Map<String, List<Map<String, String>>> CACHE = new ConcurrentHashMap<>();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .followRedirects(false)
            .build();

    @Override
    public ContentInfo getRandomContent() throws IOException {
        // Pick a random board.
        String board = BOARDS.get(RANDOM.nextInt(BOARDS.size()));

        // If no cached data for the board or cache is empty, fetch the catalog.
        if (!CACHE.containsKey(board) || CACHE.get(board).isEmpty()) {
            String url = String.format("https://a.4cdn.org/%s/catalog.json", board);
            String response = ImageUtils.httpGet(url);
            JSONArray pages = new JSONArray(response);
            List<Integer> threadNumbers = new ArrayList<>();

            for (int i = 0; i < pages.length(); i++) {
                JSONObject page = pages.getJSONObject(i);
                JSONArray threads = page.getJSONArray("threads");
                for (int j = 0; j < threads.length(); j++) {
                    threadNumbers.add(threads.getJSONObject(j).getInt("no"));
                }
            }

            // Pick a random thread.
            int thread = threadNumbers.get(RANDOM.nextInt(threadNumbers.size()));
            url = String.format("https://a.4cdn.org/%s/thread/%d.json", board, thread);
            response = ImageUtils.httpGet(url);
            JSONObject threadData = new JSONObject(response);
            JSONArray posts = threadData.getJSONArray("posts");
            List<Map<String, String>> images = new ArrayList<>();

            for (int i = 0; i < posts.length(); i++) {
                JSONObject post = posts.getJSONObject(i);
                if (post.has("tim") && post.has("ext")) {
                    Map<String, String> imageInfo = new HashMap<>();
                    imageInfo.put("image", String.format("https://i.4cdn.org/%s/%d%s",
                            board, post.getLong("tim"), post.getString("ext")));
                    imageInfo.put("description", String.format("🌐 Source: 4Chan\n\uD83D\uDD0D Board: %s\n\uD83D\uDD17 Thread: <%s>",
                            board, String.format("https://boards.4chan.org/%s/thread/%d", board, thread)));
                    imageInfo.put("title", "Here is your random 4Chan image!");
                    images.add(imageInfo);
                }
            }
            CACHE.put(board, images);
        }

        List<Map<String, String>> images = CACHE.get(board);
        Map<String, String> data = images.remove(RANDOM.nextInt(images.size()));
        return new ContentInfo("Random 4Chan Image", data.get("description"), data.get("image"));
    }
}
