package me.hash.mediaroulette.utils;

import java.net.*;
import java.io.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class RandomImage {

    private static final long EXPIRATION_TIME = 15 * 60 * 1000; // 15 minutes in milliseconds
    private static final String[] SUBREDDITS = {"hentai"};
    private static final Map<String, Queue<String>> IMAGE_QUEUES = new HashMap<>();
    private static final Map<String, Long> LAST_UPDATED = new HashMap<>();

    private static final String[] BOARDS = {"a", "c", "w", "m", "cgl", "cm", "n", "jp", "vp", "v", "vg", "vr", "co", "g", "tv", "k", "o", "an", "tg", "sp", "asp", "sci", "int", "out", "toy", "biz", "i", "po", "p", "ck", "ic", "wg", "mu", "fa", "3", "gd", "diy", "wsg", "s", "hc", "hm", "h", "e", "u", "d", "y", "t", "hr","gif","trv","fit","x","lit","adv","lgbt","mlp","b","r","r9k","pol","soc","s4s"};
    private static final HashMap<String, List<String>> CACHE = new HashMap<>();
    private static final Random RANDOM = new Random();

    public static String[] get4ChanImage() {
        // Select a board
        String board = BOARDS[RANDOM.nextInt(BOARDS.length)];

        // If a board's catalog has already been requested, just use that stored data instead
        if (CACHE.containsKey(board) && !CACHE.get(board).isEmpty()) {
            List<String> images = CACHE.get(board);
            String image = images.remove(RANDOM.nextInt(images.size()));
            String thread = image.split(" ")[1];
            image = image.split(" ")[0];
            return new String[]{image, thread};
        } else {
            // Request board catalog, and get a list of threads on the board
            List<Integer> threadnums = new ArrayList<>();
            JSONArray data = new JSONArray(HttpRequest.get("https://a.4cdn.org/" + board + "/catalog.json"));

            // Get a list of threads in the data
            for (int i = 0; i < data.length(); i++) {
                JSONObject page = data.getJSONObject(i);
                JSONArray threads = page.getJSONArray("threads");
                for (int j = 0; j < threads.length(); j++) {
                    JSONObject thread = threads.getJSONObject(j);
                    threadnums.add(thread.getInt("no"));
                }
            }

            // Select a thread
            int thread = threadnums.get(RANDOM.nextInt(threadnums.size()));

            // Request the thread information, and get a list of images in that thread
            List<String> imgs = new ArrayList<>();
            JSONObject pd = new JSONObject(HttpRequest.get("https://a.4cdn.org/" + board + "/thread/" + thread + ".json"));
            JSONArray posts = pd.getJSONArray("posts");
            for (int i = 0; i < posts.length(); i++) {
                JSONObject post = posts.getJSONObject(i);
                if (post.has("tim") && post.has("ext")) {
                    imgs.add("https://i.4cdn.org/" + board + "/" + post.getLong("tim") + post.getString("ext") + 
                             ' ' + 
                             ("https://boards.4chan.org/" + board + "/thread/" + thread));
                }
            }

            // Save images to cache
            CACHE.put(board, imgs);

            // Select an image
            String image = imgs.remove(RANDOM.nextInt(imgs.size()));
            String threadUrl = image.split(" ")[1];
            image = image.split(" ")[0];

            // Assemble and return the urls
            return new String[]{image, threadUrl};
        }
    }

    public static String getPicSumImage(String url) {
        try {
            // Create a URL object with the specified URL
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.connect();

            // Check if the response code is a redirect
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                // Get the redirected URL from the "Location" header field
                String redirectedUrl = conn.getHeaderField("Location");
                return redirectedUrl;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getRandomReddit(String accessToken) throws IOException {
        String subreddit = SUBREDDITS[RANDOM.nextInt(SUBREDDITS.length)];
        if (!IMAGE_QUEUES.containsKey(subreddit)) {
            IMAGE_QUEUES.put(subreddit, new LinkedList<>());
            LAST_UPDATED.put(subreddit, 0L);
        }
        Queue<String> imageQueue = IMAGE_QUEUES.get(subreddit);
        long lastUpdated = LAST_UPDATED.get(subreddit);
        if (imageQueue.isEmpty() || System.currentTimeMillis() - lastUpdated > EXPIRATION_TIME) {
            updateImageQueue(subreddit, accessToken, imageQueue);
            LAST_UPDATED.put(subreddit, System.currentTimeMillis());
        }
        return imageQueue.poll();
    }

    private static void updateImageQueue(String subreddit, String accessToken, Queue<String> imageQueue) throws IOException {
        String after = "";
        for (int i = 0; i < 2; i++) {
            String url = "https://oauth.reddit.com/r/" + subreddit + "/hot?limit=100&after=" + after;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("User-Agent", "YourAppName/0.1 by YourUsername");
            conn.connect();
            String response = new String(conn.getInputStream().readAllBytes());
            JSONObject json = new JSONObject(response);
            JSONArray posts = json.getJSONObject("data").getJSONArray("children");
            for (int j = 0; j < posts.length(); j++) {
                JSONObject post = posts.getJSONObject(j).getJSONObject("data");
                imageQueue.add(post.getString("url"));
            }
            after = json.getJSONObject("data").getString("after");
        }
    }


}
