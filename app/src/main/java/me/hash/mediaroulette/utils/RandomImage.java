package me.hash.mediaroulette.utils;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;
import java.awt.Image;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import me.hash.mediaroulette.Main;

public class RandomImage {

    private static final long EXPIRATION_TIME = 15 * 60 * 1000; // 15 minutes in milliseconds
    private static final Map<String, Queue<String>> IMAGE_QUEUES = new HashMap<>();
    private static final Map<String, Long> LAST_UPDATED = new HashMap<>();

    private static final String[] BOARDS = { "a", "c", "w", "m", "cgl", "cm", "n", "jp", "vp", "v", "vg", "vr", "co",
            "g", "tv", "k", "o", "an", "tg", "sp", "asp", "sci", "int", "out", "toy", "biz", "i", "po", "p", "ck", "ic",
            "wg", "mu", "fa", "3", "gd", "diy", "wsg", "s", "hc", "hm", "h", "e", "u", "d", "y", "t", "hr", "gif",
            "trv", "fit", "x", "lit", "adv", "lgbt", "mlp", "b", "r", "r9k", "pol", "soc", "s4s" };
    private static final HashMap<String, List<String>> CACHE = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static final String IMGUR_BASE_URL = "https://i.imgur.com/";
    private static final String IMGUR_ID_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int[] IMGUR_ID_LENGTH_RANGE = { 5, 6 };
    private static final String[] IMAGE_FORMATS = { "jpg", "png", "gif" };

    // 4Chan
    public static String[] get4ChanImage() {
        // Select a board
        String board = BOARDS[RANDOM.nextInt(BOARDS.length)];

        // If a board's catalog has already been requested, just use that stored data
        // instead
        if (CACHE.containsKey(board) && !CACHE.get(board).isEmpty()) {
            List<String> images = CACHE.get(board);
            String image = images.remove(RANDOM.nextInt(images.size()));
            String thread = image.split(" ")[1];
            image = image.split(" ")[0];
            return new String[] { image, thread };
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
            JSONObject pd = new JSONObject(
                    HttpRequest.get("https://a.4cdn.org/" + board + "/thread/" + thread + ".json"));
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
            return new String[] { image, threadUrl };
        }
    }

    // Picsum
    public static String getPicSumImage() {
        try {
            // Create a URL object with the specified URL
            URL obj = new URL("https://picsum.photos/1920/1080");
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.connect();

            // Check if the response code is a redirect
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                // Get the redirected URL from the "Location" header field
                String redirectedUrl = conn.getHeaderField("Location");
                return redirectedUrl;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Imgur Section
    public static String getImgurImage() {
        String imgurId = getRandomImgurId();
        String format = IMAGE_FORMATS[RANDOM.nextInt(IMAGE_FORMATS.length)];
        String imgUrl = IMGUR_BASE_URL + imgurId + "." + format;
        Image image = null;
        try {
            URL url = new URL(imgUrl);
            image = ImageIO.read(url);
            if (image == null) {
                // Failed to read the image, try again with a different Imgur ID
                return getImgurImage();
            } else if (image.getWidth(null) == 161 || image.getHeight(null) == 81) {
                // The image has invalid dimensions, try again with a different Imgur ID
                return getImgurImage();
            }
        } catch (IOException e) {
            // Failed to read the image, try again with a different Imgur ID
            return getImgurImage();
        }
        return imgUrl;
    }

    private static String getRandomImgurId() {
        int length = RANDOM.nextInt(IMGUR_ID_LENGTH_RANGE[1] - IMGUR_ID_LENGTH_RANGE[0] + 1) + IMGUR_ID_LENGTH_RANGE[0];
        StringBuilder idBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int pos = RANDOM.nextInt(IMGUR_ID_CHARACTERS.length());
            idBuilder.append(IMGUR_ID_CHARACTERS.charAt(pos));
        }
        return idBuilder.toString();
    }

    // Reddit Section
    public static String getRandomReddit() throws IOException {
        // Get the URL of the subreddits.txt file in the resources directory
        URL resourceUrl = Main.class.getResource("/subreddits.txt");
        // Convert the URL to a Path object
        Path resourcePath = null;
        try {
            resourcePath = Paths.get(resourceUrl.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // Read a random line from the subreddits.txt file
        String subreddit = getRandomLine(resourcePath);

        if (!IMAGE_QUEUES.containsKey(subreddit)) {
            IMAGE_QUEUES.put(subreddit, new LinkedList<>());
            LAST_UPDATED.put(subreddit, 0L);
        }
        Queue<String> imageQueue = IMAGE_QUEUES.get(subreddit);
        long lastUpdated = LAST_UPDATED.get(subreddit);
        if (imageQueue.isEmpty() || System.currentTimeMillis() - lastUpdated > EXPIRATION_TIME) {
            updateImageQueue(subreddit, getAccessToken(), imageQueue);
            LAST_UPDATED.put(subreddit, System.currentTimeMillis());
        }
        return imageQueue.poll();
    }

    static String getRandomLine(Path path) throws IOException {
        // Use a RandomAccessFile to read a random line from the file
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long fileSize = file.length();
            long randomPosition = ThreadLocalRandom.current().nextLong(fileSize);
            file.seek(randomPosition);
            // Skip the current line as it may be incomplete
            file.readLine();
            // Read the next complete line
            String line = file.readLine();
            // If the line is null, it means we have reached the end of the file
            // In this case, we start from the beginning of the file and read the first line
            if (line == null) {
                file.seek(0);
                line = file.readLine();
            }
            return line;
        }
    }

    // Helper method to check if a URL is valid using a regular expression
    private static boolean isValidURL(String url) {
        // Regular expression to match valid URLs
        String regex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        // Check if the URL matches the regular expression
        return url.matches(regex);
    }

    private static void updateImageQueue(String subreddit, String accessToken, Queue<String> imageQueue)
            throws IOException {
        String after = "";
        List<String> images = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
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
            for (JSONObject post : shuffledPosts) {
                JSONObject postData = post.getJSONObject("data");
                // Check if the post_hint key is present and has the value "image", "rich:video"
                // or "hosted:video"
                if (postData.has("post_hint") && ("image".equals(postData.getString("post_hint"))
                        || "rich:video".equals(postData.getString("post_hint"))
                        || "hosted:video".equals(postData.getString("post_hint")))) {
                    String postUrl = postData.getString("url");
                    // Check if the URL is an image, video or GIF and if it is valid
                    if ((postUrl.endsWith(".jpg") || postUrl.endsWith(".jpeg") || postUrl.endsWith(".png")
                            || postUrl.endsWith(".gif") || postUrl.endsWith(".mp4") || postUrl.contains("gfycat.com")
                            || postUrl.contains("redgifs.com") || postUrl.contains("streamable.com"))
                            && isValidURL(postUrl)) {
                        images.add(postUrl);
                    }
                }
            }

            // Check if the "after" key is present and not null before attempting to
            // retrieve its value as a string
            if (json.getJSONObject("data").has("after") && !json.getJSONObject("data").isNull("after")) {
                after = json.getJSONObject("data").getString("after");
            } else {
                // Handle the case where the "after" key is not present or its value is null
                // For example, you can break out of the loop or throw an exception
                break;
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

        try {
            String response = new String(conn.getInputStream().readAllBytes());
            JSONObject json = new JSONObject(response);
            return json.getString("access_token");
        } catch (IOException e) {
            // Print the error message returned by the server
            String errorResponse = new String(conn.getErrorStream().readAllBytes());
            System.err.println(errorResponse);
            throw e;
        }
    }

    // Rule34 xxx section
    public static String getRandomRule34xxx() {
        String url = "https://rule34.xxx/index.php?page=post&s=random";
        String imageUrl = null;
        try {
            Document doc = Jsoup.connect(url).get();
            Elements image = doc.select("#image");
            if (image.size() != 0) {
                imageUrl = image.attr("src");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageUrl;
    }

    public static String getGoogleQueryImage(String query) throws IOException {
        String api_key = Main.getEnv("GOOGLE_API_KEY");
        String cse_id = Main.getEnv("GOOGLE_CX");
        Random rand = new Random();
        int start = rand.nextInt(5) + 1;
        // Encode the query string
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        URL url = new URL(String.format("https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&searchType=image&start=%d", api_key, cse_id, encodedQuery, start));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            // An error occurred, print the error message
            String error = new String(conn.getErrorStream().readAllBytes());
            System.err.println("Error: " + error);
            return null;
        }
        String response = new String(conn.getInputStream().readAllBytes());
        JSONObject json = new JSONObject(response);
        JSONArray items = json.getJSONArray("items");
        int index = rand.nextInt(items.length());
        JSONObject randomImage = items.getJSONObject(index);
        return randomImage.getString("link");
    }

}
