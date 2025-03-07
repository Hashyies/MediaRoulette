package me.hash.mediaroulette.content.reddit;

import me.hash.mediaroulette.utils.GlobalLogger;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RandomRedditService {

    // Constants for caching and fetching posts
    private static final long EXPIRATION_TIME = 15 * 60 * 1000; // 15 minutes in milliseconds
    private static final int POST_LIMIT = 100; // Increase limit on posts fetched
    private static final Map<String, Queue<HashMap<String, String>>> IMAGE_QUEUES = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_UPDATED = new ConcurrentHashMap<>();
    private static final Logger logger = GlobalLogger.getLogger();

    private final RedditClient redditClient;
    private final SubredditManager subredditManager;

    public RandomRedditService(RedditClient redditClient, SubredditManager subredditManager) {
        this.redditClient = redditClient;
        this.subredditManager = subredditManager;
    }

    public HashMap<String, String> getRandomReddit(String subreddit) throws IOException, ExecutionException, InterruptedException {
        logger.log(Level.INFO, "Fetching random Reddit image from subreddit: {0}", subreddit);

        // Validate subreddit input
        if (subreddit == null || !subredditManager.doesSubredditExist(subreddit)) {
            subreddit = subredditManager.getRandomSubreddit();
            logger.log(Level.WARNING, "Fetched random subreddit: {0}", subreddit);
        }

        // Initialize queues for the subreddit
        IMAGE_QUEUES.computeIfAbsent(subreddit, k -> new ConcurrentLinkedQueue<>());
        LAST_UPDATED.computeIfAbsent(subreddit, k -> 0L);

        Queue<HashMap<String, String>> imageQueue = IMAGE_QUEUES.get(subreddit);
        long lastUpdated = LAST_UPDATED.get(subreddit);

        // Check if we need to refresh the image queue
        if (imageQueue.isEmpty() || System.currentTimeMillis() - lastUpdated > EXPIRATION_TIME) {
            logger.log(Level.INFO, "Updating image queue for subreddit: {0}", subreddit);
            updateImageQueue(subreddit);
            LAST_UPDATED.put(subreddit, System.currentTimeMillis());
        }

        HashMap<String, String> result = imageQueue.poll();

        if (result == null) {
            logger.log(Level.WARNING, "No images available for subreddit {0} after updating.", subreddit);
        } else {
            logger.log(Level.INFO, "Successfully retrieved an image from subreddit {0}.", subreddit);
        }

        return result;
    }

    private void updateImageQueue(String subreddit) {
        ExecutorService executorService = Executors.newFixedThreadPool(2); // Increase thread pool if necessary
        List<Future<List<HashMap<String, String>>>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) { // Fetch two batches
            int batchNum = i; // For use in URL
            futures.add(executorService.submit(() -> fetchImagesFromSubreddit(subreddit, batchNum)));
        }

        for (Future<List<HashMap<String, String>>> future : futures) {
            try {
                List<HashMap<String, String>> images = future.get();
                if (!images.isEmpty()) {
                    Collections.shuffle(images); // Shuffle for random distribution
                    IMAGE_QUEUES.get(subreddit).addAll(images);
                } else {
                    logger.log(Level.WARNING, "No valid images found for subreddit: {0}", subreddit);
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "Error fetching images for subreddit {0}: {1}", new Object[]{subreddit, e.getMessage()});
                Thread.currentThread().interrupt(); // Restore interrupted state
            }
        }

        executorService.shutdown();
    }

    private List<HashMap<String, String>> fetchImagesFromSubreddit(String subreddit, int batchNum) throws IOException, ExecutionException, InterruptedException {
        String accessToken = redditClient.getAccessToken();
        String after = (batchNum == 0) ? "" : "&after=" + batchNum; // Adjust pagination handling here
        String url = String.format("https://oauth.reddit.com/r/%s/hot?limit=%d%s", subreddit, POST_LIMIT, after);

        Response response = redditClient.sendGetRequestAsync(url, accessToken).get();
        if (!response.isSuccessful()) {
            logger.log(Level.SEVERE, "Failed to fetch posts for subreddit: {0}", subreddit);
            return Collections.emptyList();
        }

        String responseBody = response.body().string();
        JSONObject json = new JSONObject(responseBody);
        JSONArray posts = json.getJSONObject("data").getJSONArray("children");

        return postsToImageList(posts);
    }

    private List<HashMap<String, String>> postsToImageList(JSONArray posts) {
        List<HashMap<String, String>> images = new ArrayList<>();
        for (int i = 0; i < posts.length(); i++) {
            JSONObject postData = posts.getJSONObject(i).getJSONObject("data");
            HashMap<String, String> postDetails = processPostData(postData);
            if (postDetails != null) {
                images.add(postDetails);
            }
        }
        return images;
    }

    private HashMap<String, String> processPostData(JSONObject postData) {
        HashMap<String, String> postDetails = new HashMap<>();
        String postHint = postData.optString("post_hint", "");

        // Extract image URLs from various post types
        if ("image".equals(postHint) || "rich:video".equals(postHint) || "hosted:video".equals(postHint)) {
            String postUrl = postData.getString("url");
            if (isValidMediaUrl(postUrl)) {
                postDetails.put("image", postUrl); // Direct image URL
                postDetails.put("description", getDescription(postData));
                return postDetails;
            }
        } else if ("link".equals(postHint) || postData.has("selftext")) {
            postDetails.put("title", postData.getString("title"));
            postDetails.put("description", getDescription(postData));

            // Check for valid URL or use the selftext if applicable
            if (postData.has("url") && isValidMediaUrl(postData.getString("url"))) {
                postDetails.put("image", postData.getString("url"));
            } else {
                postDetails.put("image", "attachment://image.png"); // Fallback option, adjust logic as necessary
            }
        }

        return postDetails.isEmpty() ? null : postDetails;
    }

    private String getDescription(JSONObject postData) {
        return String.format("🌐 Source: Reddit\n🔎 Subreddit: %s\n✏️ Title: %s\n🔗 Post Link: <%s>",
                postData.getString("subreddit"), postData.getString("title"), "https://www.reddit.com" + postData.getString("permalink"));
    }

    private boolean isValidMediaUrl(String url) {
        // Check for directly accessible media URLs
        return url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") || url.endsWith(".gif") ||
                url.endsWith(".mp4") || url.contains("giphy.com") || url.contains("tenor.com") ||
                url.contains("gfycat.com") || url.contains("redgifs.com") || url.contains("streamable.com");
    }
}