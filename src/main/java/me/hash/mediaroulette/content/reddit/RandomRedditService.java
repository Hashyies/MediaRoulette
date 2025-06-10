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

    // Constants for caching and fetching posts.
    private static final long EXPIRATION_TIME = 15 * 60 * 1000; // 15 minutes in milliseconds
    private static final int POST_LIMIT = 100; // Limit on posts fetched
    // Use concurrent collections for thread-safe caching and queues.
    private static final Map<String, Queue<Map<String, String>>> IMAGE_QUEUES = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_UPDATED = new ConcurrentHashMap<>();
    private static final Logger logger = GlobalLogger.getLogger();

    // Shared executor service for fetching images concurrently.
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private final RedditClient redditClient;
    private final SubredditManager subredditManager;

    public RandomRedditService(RedditClient redditClient, SubredditManager subredditManager) {
        this.redditClient = redditClient;
        this.subredditManager = subredditManager;
    }

    public Map<String, String> getRandomReddit(String subreddit) throws IOException, ExecutionException, InterruptedException {
        logger.log(Level.INFO, "Fetching random Reddit image from subreddit: {0}", subreddit);

        // Validate subreddit input.
        if (subreddit == null || !subredditManager.doesSubredditExist(subreddit)) {
            subreddit = subredditManager.getRandomSubreddit();
            logger.log(Level.WARNING, "Invalid subreddit provided. Fetched random subreddit: {0}", subreddit);
        }

        // Initialize caches for the subreddit if absent.
        IMAGE_QUEUES.computeIfAbsent(subreddit, k -> new ConcurrentLinkedQueue<>());
        LAST_UPDATED.computeIfAbsent(subreddit, k -> 0L);

        Queue<Map<String, String>> imageQueue = IMAGE_QUEUES.get(subreddit);
        long lastUpdated = LAST_UPDATED.get(subreddit);

        // Refresh the image queue if it is empty or expired.
        if (imageQueue.isEmpty() || System.currentTimeMillis() - lastUpdated > EXPIRATION_TIME) {
            logger.log(Level.INFO, "Updating image queue for subreddit: {0}", subreddit);
            updateImageQueue(subreddit);
            LAST_UPDATED.put(subreddit, System.currentTimeMillis());
        }

        Map<String, String> result = imageQueue.poll();
        if (result == null) {
            logger.log(Level.WARNING, "No images available for subreddit {0} after updating.", subreddit);
        } else {
            logger.log(Level.INFO, "Successfully retrieved an image from subreddit {0}.", subreddit);
        }
        return result;
    }

    private void updateImageQueue(String subreddit) {
        List<CompletableFuture<List<Map<String, String>>>> futures = new ArrayList<>();

        // Fetch two batches concurrently using CompletableFuture.
        for (int i = 0; i < 2; i++) {
            final int batchNum = i;
            CompletableFuture<List<Map<String, String>>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchImagesFromSubreddit(subreddit, batchNum);
                } catch (IOException | InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, "Error fetching images for subreddit {0} in batch {1}: {2}",
                            new Object[]{subreddit, batchNum, e.getMessage()});
                    Thread.currentThread().interrupt();
                    return Collections.emptyList();
                }
            }, executorService);
            futures.add(future);
        }

        // Wait for all tasks to complete and add fetched images to the queue.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        for (CompletableFuture<List<Map<String, String>>> future : futures) {
            try {
                List<Map<String, String>> images = future.get();
                if (!images.isEmpty()) {
                    Collections.shuffle(images); // Shuffle to ensure random distribution.
                    IMAGE_QUEUES.get(subreddit).addAll(images);
                } else {
                    logger.log(Level.WARNING, "No valid images found for subreddit: {0}", subreddit);
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "Error processing image batch for subreddit {0}: {1}",
                        new Object[]{subreddit, e.getMessage()});
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<Map<String, String>> fetchImagesFromSubreddit(String subreddit, int batchNum)
            throws IOException, ExecutionException, InterruptedException {
        String accessToken = redditClient.getAccessToken();
        // For pagination – here simply using the batch number.
        String after = (batchNum == 0) ? "" : "&after=" + batchNum;
        String url = String.format("https://oauth.reddit.com/r/%s/hot?limit=%d%s", subreddit, POST_LIMIT, after);

        Response response = redditClient.sendGetRequestAsync(url, accessToken).get();
        if (!response.isSuccessful()) {
            logger.log(Level.SEVERE, "Failed to fetch posts for subreddit: {0}", subreddit);
            return Collections.emptyList();
        }

        String responseBody = response.body().string();
        response.close(); // Ensure the response is closed.
        JSONObject json = new JSONObject(responseBody);
        JSONArray posts = json.getJSONObject("data").getJSONArray("children");

        return postsToImageList(posts);
    }

    private List<Map<String, String>> postsToImageList(JSONArray posts) {
        List<Map<String, String>> images = new ArrayList<>();
        for (int i = 0; i < posts.length(); i++) {
            JSONObject postData = posts.getJSONObject(i).getJSONObject("data");
            Map<String, String> postDetails = processPostData(postData);
            if (postDetails != null) {
                images.add(postDetails);
            }
        }
        return images;
    }

    private Map<String, String> processPostData(JSONObject postData) {
        Map<String, String> postDetails = new HashMap<>();
        logger.log(Level.FINE, "Processing post data: {0}", postData);

        String imageUrl = null;

        // 1. For gallery posts, iterate over items and pick the highest quality image.
        if (postData.optBoolean("is_gallery", false)) {
            JSONObject galleryData = postData.optJSONObject("gallery_data");
            JSONObject mediaMetadata = postData.optJSONObject("media_metadata");
            if (galleryData != null && mediaMetadata != null) {
                JSONArray items = galleryData.optJSONArray("items");
                if (items != null && items.length() > 0) {
                    double bestArea = 0;
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        String mediaId = item.getString("media_id");
                        if (mediaMetadata.has(mediaId)) {
                            JSONObject media = mediaMetadata.getJSONObject(mediaId);
                            JSONObject sObj = media.optJSONObject("s");
                            if (sObj != null) {
                                String candidateUrl = sObj.optString("u", "").replaceAll("&amp;", "&");
                                int width = sObj.optInt("x", 0);
                                int height = sObj.optInt("y", 0);
                                double area = width * height;
                                if (isValidMediaUrl(candidateUrl) && area > bestArea) {
                                    bestArea = area;
                                    imageUrl = candidateUrl;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. For video posts, avoid using the video URL.
        // Instead, check for a high-quality preview image.
        String postHint = postData.optString("post_hint", "");
        if (imageUrl == null && ("rich:video".equals(postHint) || "hosted:video".equals(postHint))) {
            if (postData.has("preview")) {
                JSONObject preview = postData.getJSONObject("preview");
                JSONArray images = preview.optJSONArray("images");
                if (images != null && images.length() > 0) {
                    double bestArea = 0;
                    for (int i = 0; i < images.length(); i++) {
                        JSONObject imgObj = images.getJSONObject(i);
                        JSONObject source = imgObj.optJSONObject("source");
                        if (source != null) {
                            String candidateUrl = source.optString("url", "").replaceAll("&amp;", "&");
                            int width = source.optInt("width", 0);
                            int height = source.optInt("height", 0);
                            double area = width * height;
                            if (isValidMediaUrl(candidateUrl) && area > bestArea) {
                                bestArea = area;
                                imageUrl = candidateUrl;
                            }
                        }
                    }
                }
            }
        }

        // 3. For standard image posts (or non-video posts), check if preview provides a higher-quality image.
        if (imageUrl == null && postData.has("url") && isValidMediaUrl(postData.getString("url"))) {
            if (postData.has("preview")) {
                JSONObject preview = postData.getJSONObject("preview");
                JSONArray images = preview.optJSONArray("images");
                if (images != null && images.length() > 0) {
                    double bestArea = 0;
                    for (int i = 0; i < images.length(); i++) {
                        JSONObject imgObj = images.getJSONObject(i);
                        JSONObject source = imgObj.optJSONObject("source");
                        if (source != null) {
                            String candidateUrl = source.optString("url", "").replaceAll("&amp;", "&");
                            int width = source.optInt("width", 0);
                            int height = source.optInt("height", 0);
                            double area = width * height;
                            if (isValidMediaUrl(candidateUrl) && area > bestArea) {
                                bestArea = area;
                                imageUrl = candidateUrl;
                            }
                        }
                    }
                } else {
                    // Fallback to the URL if no preview image is present.
                    imageUrl = postData.getString("url");
                }
            } else {
                imageUrl = postData.getString("url");
            }
        }

        // 4. Fallback: Check the thumbnail field.
        if (imageUrl == null && postData.has("thumbnail")) {
            String thumbnail = postData.getString("thumbnail");
            if (thumbnail.startsWith("http") && isValidMediaUrl(thumbnail)) {
                imageUrl = thumbnail;
            }
        }

        // 5. If still no image is found, use a default image and mark it for creation.
        if (imageUrl == null) {
            imageUrl = "attachment://image.png";
            postDetails.put("image_type", "create");
        }

        postDetails.put("image", imageUrl);
        postDetails.put("description", getDescription(postData));
        if (postData.has("title")) {
            postDetails.put("title", postData.getString("title"));
        }
        return postDetails;
    }

    private String getDescription(JSONObject postData) {
        return String.format("🌐 Source: Reddit\n🔎 Subreddit: %s\n✏️ Title: %s\n🔗 Post Link: <%s>",
                postData.getString("subreddit"),
                postData.getString("title"),
                "https://www.reddit.com" + postData.getString("permalink"));
    }

    private boolean isValidMediaUrl(String url) {
        // Check for directly accessible media URLs.
        return url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") ||
                url.endsWith(".gif") || url.endsWith(".mp4") ||
                url.contains("giphy.com") || url.contains("tenor.com") ||
                url.contains("gfycat.com") || url.contains("redgifs.com") || url.contains("streamable.com");
    }
}
