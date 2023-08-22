package me.hash.mediaroulette.utils.random;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import me.hash.mediaroulette.Main;

public class RandomReddit {

    private static String accessToken = null;
    private static long accessTokenExpirationTime = 0;
    private static final long EXPIRATION_TIME = 15 * 60 * 1000; // 15 minutes in milliseconds
    private static final Map<String, Queue<String>> IMAGE_QUEUES = new HashMap<>();
    private static final Map<String, Long> LAST_UPDATED = new HashMap<>();

    // Fix This method for jars
    public static String getRandomReddit(String subreddit) throws IOException, URISyntaxException {
        if (subreddit == null || !doesSubredditExist(subreddit)) {
            // Generate a random subreddit from the subreddits.txt file
            InputStream inputStream = Main.class.getResourceAsStream("/subreddits.txt");
            subreddit = getRandomLine(inputStream);
        }
        
        if (!IMAGE_QUEUES.containsKey(subreddit)) {
            IMAGE_QUEUES.put(subreddit, new LinkedList<>());
            LAST_UPDATED.put(subreddit, 0L);
        }
        
        Queue<String> imageQueue = IMAGE_QUEUES.get(subreddit);
        long lastUpdated = LAST_UPDATED.get(subreddit);
        
        if (imageQueue.isEmpty() || System.currentTimeMillis() - lastUpdated > EXPIRATION_TIME) {
            // Only renew the access token when it has expired
            if (accessToken == null || System.currentTimeMillis() > accessTokenExpirationTime) {
                accessToken = getAccessToken();
            }
            updateImageQueue(subreddit, accessToken, imageQueue);
            LAST_UPDATED.put(subreddit, System.currentTimeMillis());
        }
        
        return imageQueue.poll();
    }
    
    public static boolean doesSubredditExist(String subreddit) throws IOException {
        String url = "https://www.reddit.com/r/" + subreddit + "/about.json";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("HEAD");
        conn.setRequestProperty("User-Agent", "MediaRoulette/0.1 by pgmmestar");
        conn.setConnectTimeout(5000); // Set a timeout of 5 seconds
        conn.setReadTimeout(5000); // Set a timeout of 5 seconds
        conn.connect();
        int responseCode = conn.getResponseCode();
        return responseCode == 200;
    }    

    private static String getRandomLine(InputStream inputStream) throws IOException {
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

    // Helper method to check if a URL is valid using a regular expression
    private static boolean isValidURL(String url) {
        // Regular expression to match valid URLs
        String regex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        // Check if the URL matches the regular expression
        return url.matches(regex);
    }

    private static void updateImageQueue(String subreddit, String accessToken, Queue<String> imageQueue) {
        String after = "";
        List<String> images = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            try {
                String url = "https://oauth.reddit.com/r/" + subreddit + "/hot?limit=100&after=" + after;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("User-Agent", "MediaRoulette/0.1 by pgmmestar");
                conn.setConnectTimeout(5000); // Set a timeout of 5 seconds
                conn.setReadTimeout(5000); // Set a timeout of 5 seconds
                conn.setRequestProperty("Accept-Encoding", "gzip"); // Request compressed data to reduce bandwidth usage
                conn.connect();
                // Use a GZIPInputStream to decompress the data if it is compressed
                InputStream inputStream = conn.getInputStream();
                if ("gzip".equals(conn.getContentEncoding())) {
                    inputStream = new GZIPInputStream(inputStream);
                }
                String response = new String(inputStream.readAllBytes());
                JSONObject json = new JSONObject(response);
                JSONArray posts = json.getJSONObject("data").getJSONArray("children");
                // Shuffle the posts to make them more random
                List<JSONObject> shuffledPosts = new ArrayList<>();
                for (int j = 0; j < posts.length(); j++) {
                    shuffledPosts.add(posts.getJSONObject(j));
                }
                Collections.shuffle(shuffledPosts);

                // Use stream() to filter and collect the image URLs
                List<String> postUrls = shuffledPosts.stream()
                        .map(post -> post.getJSONObject("data"))
                        .filter(postData -> postData.has("post_hint")
                                && ("image".equals(postData.getString("post_hint"))
                                        || "rich:video".equals(postData.getString("post_hint"))
                                        || "hosted:video".equals(postData.getString("post_hint"))))
                        .map(postData -> postData.getString("url"))
                        .filter(postUrl -> (postUrl.endsWith(".jpg") || postUrl.endsWith(".jpeg")
                                || postUrl.endsWith(".png")
                                || postUrl.endsWith(".gif") || postUrl.endsWith(".mp4")
                                || postUrl.contains("gfycat.com")
                                || postUrl.contains("redgifs.com") || postUrl.contains("streamable.com"))
                                && isValidURL(postUrl))
                        .collect(Collectors.toList());

                images.addAll(postUrls);

                // Check if the "after" key is present and not null before attempting to
                // retrieve its value as a string
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

        URL url = new URL("https://www.reddit.com/api/v1/access_token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + encodedAuthString);
        conn.setRequestProperty("User-Agent", "MediaRoulette/0.1 by pgmmestar");
        conn.setDoOutput(true);
        conn.getOutputStream().write(("grant_type=password&username=" + Main.getEnv("REDDIT_USERNAME") +
                "&password=" + Main.getEnv("REDDIT_PASSWORD")).getBytes());

        try (InputStream inputStream = conn.getInputStream()) {
            String response = new String(inputStream.readAllBytes());
            JSONObject json = new JSONObject(response);
            // Set the expiration time of the access token
            accessTokenExpirationTime = System.currentTimeMillis() + json.getLong("expires_in") * 1000;
            return json.getString("access_token");
        } catch (IOException e) {
            // Print the error message returned by the server
            try (InputStream errorStream = conn.getErrorStream()) {
                String errorResponse = new String(errorStream.readAllBytes());
                System.err.println(errorResponse);
            }
            throw e;
        }
    }

}