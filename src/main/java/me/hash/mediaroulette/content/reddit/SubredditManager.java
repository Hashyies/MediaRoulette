package me.hash.mediaroulette.content.reddit;

import okhttp3.Response;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SubredditManager {

    // Cache to avoid repeatedly checking for subreddit existence.
    private static final Map<String, Boolean> SUBREDDIT_EXISTS_CACHE = new ConcurrentHashMap<>();
    private final RedditClient redditClient;

    public SubredditManager(RedditClient redditClient) {
        this.redditClient = redditClient;
    }

    public boolean doesSubredditExist(String subreddit) throws IOException {
        if (SUBREDDIT_EXISTS_CACHE.containsKey(subreddit)) {
            return SUBREDDIT_EXISTS_CACHE.get(subreddit);
        }

        String url = "https://oauth.reddit.com/r/" + subreddit + "/about";
        Response response = redditClient.sendGetRequestAsync(url, redditClient.getAccessToken()).join();
        String responseBody = response.body().string();
        response.close();

        JSONObject json = new JSONObject(responseBody);
        // If an error key exists, then the subreddit likely does not exist.
        boolean exists = !json.has("error");
        SUBREDDIT_EXISTS_CACHE.put(subreddit, exists);

        // Simple cache eviction policy.
        if (SUBREDDIT_EXISTS_CACHE.size() > 1000) {
            SUBREDDIT_EXISTS_CACHE.clear();
        }
        return exists;
    }

    public String getRandomSubreddit() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("subreddits.txt"))
        ))) {
            List<String> subreddits = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                subreddits.add(line.trim());
            }
            if (subreddits.isEmpty()) {
                throw new IOException("No subreddits available in the list.");
            }
            Collections.shuffle(subreddits);
            return subreddits.get(0);
        }
    }
}