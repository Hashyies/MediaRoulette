package me.hash.mediaroulette.utils.random.reddit;

import me.hash.mediaroulette.utils.GlobalLogger;
import me.hash.mediaroulette.utils.user.User;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RandomRedditService {

    private static final long EXPIRATION_TIME = 15 * 60 * 1000; // 15 minutes in milliseconds
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
        // Log initial subreddit input
        logger.log(Level.INFO, "Fetching random Reddit image from subreddit: {0}", subreddit);

        // If subreddit is null or does not exist, get a random one
        if (subreddit == null || !subredditManager.doesSubredditExist(subreddit)) {
            subreddit = subredditManager.getRandomSubreddit();
            logger.log(Level.WARNING, "Provided subreddit was null or non-existent. Fetched random subreddit: {0}", subreddit);

            // Check if we still have a null subreddit
            if (subreddit == null) {
                logger.log(Level.SEVERE, "Random subreddit could not be retrieved. Aborting.");
                throw new IllegalStateException("Failed to retrieve a valid subreddit.");
            }
        }

        // Initialize image queue and last updated time for the subreddit if not present
        IMAGE_QUEUES.computeIfAbsent(subreddit, k -> new LinkedList<>());
        LAST_UPDATED.computeIfAbsent(subreddit, k -> 0L);

        Queue<HashMap<String, String>> imageQueue = IMAGE_QUEUES.get(subreddit);
        long lastUpdated = LAST_UPDATED.get(subreddit);

        // Check if the image queue is empty or the cache has expired
        if (imageQueue.isEmpty() || System.currentTimeMillis() - lastUpdated > EXPIRATION_TIME) {
            logger.log(Level.INFO, "Image queue for subreddit {0} is empty or expired. Updating queue.", subreddit);

            String accessToken = redditClient.getAccessToken();

            try {
                updateImageQueue(subreddit, accessToken, imageQueue);
                LAST_UPDATED.put(subreddit, System.currentTimeMillis());
            } catch (IOException | ExecutionException | InterruptedException e) {
                logger.log(Level.SEVERE, "Error updating image queue for subreddit {0}: {1}", new Object[]{subreddit, e.getMessage()});
                throw e; // rethrow the exception after logging
            }
        }

        HashMap<String, String> result = imageQueue.poll();

        // Null check for result
        if (result == null) {
            logger.log(Level.WARNING, "No images available for subreddit {0} after updating.", subreddit);
            return null;  // Return null if no image is available
        } else {
            // Log the successful retrieval of an image
            logger.log(Level.INFO, "Successfully retrieved an image from subreddit {0}.", subreddit);
        }

        return result;
    }

    private void updateImageQueue(String subreddit, String accessToken, Queue<HashMap<String, String>> imageQueue) throws IOException, ExecutionException, InterruptedException {
        String after = "";
        List<HashMap<String, String>> images = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            String url = "https://oauth.reddit.com/r/" + subreddit + "/hot?limit=100&after=" + after;
            Response response = redditClient.sendGetRequestAsync(url, accessToken).get();

            if (!response.isSuccessful()) {
                logger.log(Level.SEVERE, "Failed to fetch posts for subreddit: {0}", subreddit);
                return;  // Exit the method if fetching fails
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONArray posts = json.getJSONObject("data").getJSONArray("children");

            // Add posts to images list
            images.addAll(postsToImageList(posts));

            if (json.getJSONObject("data").has("after") && !json.getJSONObject("data").isNull("after")) {
                after = json.getJSONObject("data").getString("after");
            } else {
                break;
            }
        }

        // Only shuffle and add to the queue if images are not empty
        if (!images.isEmpty()) {
            Collections.shuffle(images);
            imageQueue.addAll(images);
        } else {
            logger.log(Level.WARNING, "No valid images found for subreddit: {0}", subreddit);
        }
    }

    private List<HashMap<String, String>> postsToImageList(JSONArray posts) {
        List<JSONObject> shuffledPosts = new ArrayList<>();
        System.out.println(posts.toString());
        for (int j = 0; j < posts.length(); j++) {
            shuffledPosts.add(posts.getJSONObject(j));
        }

        Collections.shuffle(shuffledPosts);

        return shuffledPosts.stream()
                .map(post -> post.getJSONObject("data"))
                .filter(postData -> postData.has("post_hint") || postData.has("selftext")) // Adjusted to ensure text posts are considered
                .map(postData -> {
                    HashMap<String, String> postDetails = new HashMap<>();
                    String postHint = postData.optString("post_hint", "");

                    // Extract post information based on type
                    if ("image".equals(postHint) || "rich:video".equals(postHint) || "hosted:video".equals(postHint)) {
                        // Handle image or video posts
                        String postUrl = postData.getString("url");
                        if (isValidMediaUrl(postUrl)) {
                            postDetails.put("image", postUrl);
                            postDetails.put("description", String.format("🌐 Source: Reddit\n"
                                            + "🔎 Subreddit: %s\n"
                                            + "✏️ Title: %s\n"
                                            + "🔗 Post Link: <%s>",
                                    postData.getString("subreddit"), postData.getString("title"), "https://www.reddit.com" + postData.getString("permalink")));
                            postDetails.put("title", "Here is your random Reddit image!");
                        }
                    } else if ("link".equals(postHint) || "self".equals(postHint) || postData.has("selftext")) {
                        // Handle link or text (self) posts
                        String postTitle = postData.getString("title");
                        String postLink = "https://www.reddit.com" + postData.getString("permalink");

                        postDetails.put("title", postTitle);
                        postDetails.put("description", String.format("""
                                    🌐 Source: Reddit
                                    🔎 Subreddit: %s
                                    ✏️ Title: %s
                                    🔗 Post Link: <%s>""",
                                postData.getString("subreddit"), postTitle, postLink));

                        if (postData.has("selftext") && !postData.getString("selftext").isEmpty()) {
                            postDetails.put("text", postData.getString("selftext"));
                            postDetails.put("image", "attachment://image.png");
                            postDetails.put("image_type", "create");
                            postDetails.put("image_content", postData.getString("subreddit"));
                        } else {
                            postDetails.put("image", postData.getString("url"));
                        }
                    }

                    return postDetails.isEmpty() ? null : postDetails;  // Return null if the post is not valid
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }



    private boolean isValidMediaUrl(String url) {
        return url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") || url.endsWith(".gif") || url.endsWith(".mp4")
                || url.contains("gfycat.com") || url.contains("redgifs.com") || url.contains("streamable.com");
    }

    public String getRandomLine(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<>();
        try (Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(lines.size());
        return lines.get(randomIndex);
    }
}
