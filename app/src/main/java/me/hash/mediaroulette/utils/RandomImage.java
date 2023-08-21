package me.hash.mediaroulette.utils;

import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.Image;
import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import me.hash.mediaroulette.Main;

public class RandomImage {

    public static final List<String> BOARDS = Arrays.asList("a", "c", "w", "m", "cgl", "cm", "n", "jp", "vp", "v", "vg", "vr", "co",
            "g", "tv", "k", "o", "an", "tg", "sp", "asp", "sci", "int", "out", "toy", "biz", "i", "po", "p", "ck", "ic",
            "wg", "mu", "fa", "3", "gd", "diy", "wsg", "s", "hc", "hm", "h", "e", "u", "d", "y", "t", "hr", "gif",
            "trv", "fit", "x", "lit", "adv", "lgbt", "mlp", "b", "r", "r9k", "pol", "soc", "s4s");
    private static final HashMap<String, List<String>> CACHE = new HashMap<>();
    private static final Random RANDOM = new Random();

    // 4Chan

    public static String[] get4ChanImage(String board) {
        // Select a board if not provided
        if (board == null) {
            board = BOARDS.get(RANDOM.nextInt(BOARDS.size()));
        }

        // Check if the board's catalog has already been requested
        if (!CACHE.containsKey(board) || CACHE.get(board).isEmpty()) {
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
        }

        List<String> images = CACHE.get(board);
        String image = images.remove(RANDOM.nextInt(images.size()));
        String threadUrl = image.split(" ")[1];
        image = image.split(" ")[0];

        // Assemble and return the urls
        return new String[] { image, threadUrl };
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
        String[] IMAGE_FORMATS = { "jpg", "png", "gif" };
        String imgurId = getRandomImgurId();
        String imgUrl = "https://i.imgur.com/" + imgurId + "." + IMAGE_FORMATS[RANDOM.nextInt(IMAGE_FORMATS.length)];
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
        String IMGUR_ID_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int[] IMGUR_ID_LENGTH_RANGE = { 5, 6 };
        int length = RANDOM.nextInt(IMGUR_ID_LENGTH_RANGE[1] - IMGUR_ID_LENGTH_RANGE[0] + 1) + IMGUR_ID_LENGTH_RANGE[0];
        StringBuilder idBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int pos = RANDOM.nextInt(IMGUR_ID_CHARACTERS.length());
            idBuilder.append(IMGUR_ID_CHARACTERS.charAt(pos));
        }
        return idBuilder.toString();
    }

    // Rule34 xxx section (Add random for tags)
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
        URL url = new URL(
                String.format("https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&searchType=image&start=%d",
                        api_key, cse_id, encodedQuery, start));
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