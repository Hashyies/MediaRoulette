package me.hash.mediaroulette.content.provider.impl.images;

import me.hash.mediaroulette.model.content.MediaResult;
import me.hash.mediaroulette.model.content.MediaSource;
import me.hash.mediaroulette.content.provider.MediaProvider;
import me.hash.mediaroulette.content.http.HttpClientWrapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FourChanProvider implements MediaProvider {
    private static final List<String> BOARDS = Arrays.asList("a", "c", "w", "m", "cgl", "cm", "n", "jp", "vp", "v", "vg",
            "vr", "co", "g", "tv", "k", "o", "an", "tg", "sp", "asp", "sci", "int", "out", "toy", "biz", "i", "po", "p", "ck", "ic",
            "wg", "mu", "fa", "3", "gd", "diy", "wsg", "s", "hc", "hm", "h", "e", "u", "d", "y", "t", "hr", "gif",
            "trv", "fit", "x", "lit", "adv", "lgbt", "mlp", "b", "r", "r9k", "pol", "soc", "s4s");

    private final Map<String, Queue<MediaResult>> imageCache = new ConcurrentHashMap<>();
    private final HttpClientWrapper httpClient;
    private final Random random = new Random();

    public FourChanProvider(HttpClientWrapper httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public MediaResult getRandomMedia(String board) throws IOException {
        if (board == null || !BOARDS.contains(board)) {
            board = BOARDS.get(random.nextInt(BOARDS.size()));
        }

        Queue<MediaResult> cache = imageCache.computeIfAbsent(board, k -> new LinkedList<>());

        if (cache.isEmpty()) {
            populateCache(board);
        }

        MediaResult result = cache.poll();
        if (result == null) {
            throw new IOException("No images available for board: " + board);
        }
        return result;
    }

    private void populateCache(String board) throws IOException {
        List<Integer> threadNumbers = fetchThreadNumbers(board);
        if (threadNumbers.isEmpty()) {
            return;
        }

        int selectedThread = threadNumbers.get(random.nextInt(threadNumbers.size()));
        List<MediaResult> images = fetchImagesFromThread(board, selectedThread);

        Queue<MediaResult> cache = imageCache.get(board);
        cache.addAll(images);
    }

    private List<Integer> fetchThreadNumbers(String board) throws IOException {
        String url = String.format("https://a.4cdn.org/%s/catalog.json", board);
        String response = httpClient.get(url);

        JSONArray data = new JSONArray(response);
        List<Integer> threadNumbers = new ArrayList<>();

        for (int i = 0; i < data.length(); i++) {
            JSONObject page = data.getJSONObject(i);
            JSONArray threads = page.getJSONArray("threads");
            for (int j = 0; j < threads.length(); j++) {
                JSONObject thread = threads.getJSONObject(j);
                threadNumbers.add(thread.getInt("no"));
            }
        }
        return threadNumbers;
    }

    private List<MediaResult> fetchImagesFromThread(String board, int threadId) throws IOException {
        String url = String.format("https://a.4cdn.org/%s/thread/%d.json", board, threadId);
        String response = httpClient.get(url);

        JSONObject postData = new JSONObject(response);
        JSONArray posts = postData.getJSONArray("posts");
        List<MediaResult> images = new ArrayList<>();

        for (int i = 0; i < posts.length(); i++) {
            JSONObject post = posts.getJSONObject(i);
            if (post.has("tim") && post.has("ext")) {
                String imageUrl = String.format("https://i.4cdn.org/%s/%d%s",
                        board, post.getLong("tim"), post.getString("ext"));

                String description = String.format("🌐 Source: 4Chan\n" +
                                "\uD83D\uDD0D Board: %s\n" +
                                "\uD83D\uDD17 Thread: <%s>",
                        board,
                        String.format("https://boards.4chan.org/%s/thread/%d", board, threadId));

                images.add(new MediaResult(imageUrl, "Here is your random 4Chan image!", description, MediaSource.CHAN_4));
            }
        }
        return images;
    }

    public boolean isValidBoard(String board) {
        return BOARDS.contains(board);
    }

    @Override
    public boolean supportsQuery() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "4Chan";
    }
}
