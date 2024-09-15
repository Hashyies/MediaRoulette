package me.hash.mediaroulette.utils.random.reddit;

import okhttp3.Response;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SubredditManager {

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
        JSONObject json = new JSONObject(responseBody);

        boolean exists = !(json.has("error") && json.getInt("error") == 0);
        SUBREDDIT_EXISTS_CACHE.put(subreddit, exists);

        if (SUBREDDIT_EXISTS_CACHE.size() > 1000) {
            SUBREDDIT_EXISTS_CACHE.clear();  // Simple cache eviction policy
        }

        if (!exists)
            System.out.println(responseBody);

        return exists;
    }

    public String getRandomSubreddit() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("subreddits.txt"))));
        List<String> subreddits = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            subreddits.add(line);
        }

        Collections.shuffle(subreddits);
        return subreddits.get(0);
    }
}
