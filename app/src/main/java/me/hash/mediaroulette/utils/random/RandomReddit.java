package me.hash.mediaroulette.utils.random;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import me.hash.mediaroulette.Main;

public class RandomReddit {

    private static String accessToken = null;
    private static long accessTokenExpirationTime = 0;
    private static final long EXPIRATION_TIME = 15 * 60 * 1000; // 15 minutes in milliseconds
    private static final Map<String, Queue<HashMap<String, String>>> IMAGE_QUEUES = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_UPDATED = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> SUBREDDIT_EXISTS_CACHE = new ConcurrentHashMap<>();
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded");

    public static HashMap<String, String> getRandomReddit(String subreddit) throws IOException {
        if (subreddit == null || !doesSubredditExist(subreddit)) {
            InputStream inputStream = Main.class.getResourceAsStream("/subreddits.txt");
            subreddit = getRandomLine(inputStream);
        }
        
        if (!IMAGE_QUEUES.containsKey(subreddit)) {
            IMAGE_QUEUES.put(subreddit, new LinkedList<>());
            LAST_UPDATED.put(subreddit, 0L);
        }
        
        Queue<HashMap<String, String>> imageQueue = IMAGE_QUEUES.get(subreddit);
        long lastUpdated = LAST_UPDATED.get(subreddit);
        
        if (imageQueue.isEmpty() || System.currentTimeMillis() - lastUpdated > EXPIRATION_TIME) {
            if (accessToken == null || System.currentTimeMillis() > accessTokenExpirationTime) {
                accessToken = getAccessToken();
            }
            updateImageQueue(subreddit, accessToken, imageQueue);
            LAST_UPDATED.put(subreddit, System.currentTimeMillis());
        }
        
        return imageQueue.poll();
    }
    
    public static boolean doesSubredditExist(String subreddit) throws IOException {
        if (SUBREDDIT_EXISTS_CACHE.containsKey(subreddit)) {
            return SUBREDDIT_EXISTS_CACHE.get(subreddit);
        }
        
        String url = "https://www.reddit.com/r/" + subreddit + "/about.json";
        Request request = new Request.Builder()
                .url(url)
                .head()
                .addHeader("User-Agent", "MediaRoulette/0.1 by pgmmestar")
                .build();
        Response response = RandomImage.HTTP_CLIENT.newCall(request).execute();
        
        boolean exists = response.isSuccessful();
        SUBREDDIT_EXISTS_CACHE.put(subreddit, exists);
        
        if (SUBREDDIT_EXISTS_CACHE.size() > 2000) {
            Iterator<String> iterator = SUBREDDIT_EXISTS_CACHE.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        
        return exists;
    }    

    public static String getRandomLine(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(lines.size());
        return lines.get(randomIndex);
    }    

    private static void updateImageQueue(String subreddit, String accessToken, Queue<HashMap<String, String>> imageQueue) {
        String after = "";
        List<HashMap<String, String>> images = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            try {
                String url = "https://oauth.reddit.com/r/" + subreddit + "/hot?limit=100&after=" + after;
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("User-Agent", "MediaRoulette/0.1 by pgmmestar")
                        .build();
                Response response = RandomImage.HTTP_CLIENT.newCall(request).execute();
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                JSONArray posts = json.getJSONObject("data").getJSONArray("children");
                List<JSONObject> shuffledPosts = new ArrayList<>();
                for (int j = 0; j < posts.length(); j++) {
                    shuffledPosts.add(posts.getJSONObject(j));
                }
                Collections.shuffle(shuffledPosts);
    
                List<HashMap<String, String>> postUrls = shuffledPosts.parallelStream()
                        .map(post -> post.getJSONObject("data"))
                        .filter(postData -> postData.has("post_hint")
                                && ("image".equals(postData.getString("post_hint"))
                                        || "rich:video".equals(postData.getString("post_hint"))
                                        || "hosted:video".equals(postData.getString("post_hint"))))
                        .map(postData -> {
                            HashMap<String, String> postDetails = new HashMap<>();
                            String postUrl = postData.getString("url");
                            if (postUrl.endsWith(".jpg") || postUrl.endsWith(".jpeg")
                                    || postUrl.endsWith(".png")
                                    || postUrl.endsWith(".gif") || postUrl.endsWith(".mp4")
                                    || postUrl.contains("gfycat.com")
                                    || postUrl.contains("redgifs.com") || postUrl.contains("streamable.com")) {
                                postDetails.put("image", postUrl);
                                postDetails.put("description", String.format("🌐 Source: Reddit\n"
                                                                            + "🔎 Subreddit: %s\n"
                                                                            + "✏️ Title: %s\n"
                                                                            + "🔗 Post Link: <%s>", 
                                postData.getString("title"), subreddit,  "https://www.reddit.com" + postData.getString("permalink")));
                                postDetails.put("title", "Here is your random Reddit image!");
                            }
                            return postDetails;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    
                images.addAll(postUrls);
    
                if (json.getJSONObject("data").has("after") && !json.getJSONObject("data").isNull("after")) {
                    after = json.getJSONObject("data").getString("after");
                } else {
                    break;
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
        Collections.shuffle(images);
        imageQueue.addAll(images);
    }
    

    private static String getAccessToken() throws IOException {
        String authString = Main.getEnv("REDDIT_CLIENT_ID") + ":" + Main.getEnv("REDDIT_CLIENT_SECRET");
        String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());

        String url = "https://www.reddit.com/api/v1/access_token";
        RequestBody body = RequestBody.create("grant_type=password&username=" + Main.getEnv("REDDIT_USERNAME") +
                "&password=" + Main.getEnv("REDDIT_PASSWORD"), MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Basic " + encodedAuthString)
                .addHeader("User-Agent", "MediaRoulette/0.1 by pgmmestar")
                .build();
        Response response = RandomImage.HTTP_CLIENT.newCall(request).execute();
        String responseBody = response.body().string();
        JSONObject json = new JSONObject(responseBody);
        accessTokenExpirationTime = System.currentTimeMillis() + json.getLong("expires_in") * 1000;
        return json.getString("access_token");
    }

}