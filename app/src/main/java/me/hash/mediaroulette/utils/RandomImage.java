package me.hash.mediaroulette.utils;

import java.awt.Image;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import me.hash.mediaroulette.Main;

public class RandomImage {

    private static final Map<String, List<String>> CACHE = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    public static OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .followRedirects(false)
            .build();
    public static final List<String> BOARDS = Arrays.asList("a", "c", "w", "m", "cgl", "cm", "n", "jp", "vp", "v", "vg", "vr", "co",
            "g", "tv", "k", "o", "an", "tg", "sp", "asp", "sci", "int", "out", "toy", "biz", "i", "po", "p", "ck", "ic",
            "wg", "mu", "fa", "3", "gd", "diy", "wsg", "s", "hc", "hm", "h", "e", "u", "d", "y", "t", "hr", "gif",
            "trv", "fit", "x", "lit", "adv", "lgbt", "mlp", "b", "r", "r9k", "pol", "soc", "s4s");

    public static String[] get4ChanImage(String board) {
        // Select a board if not provided
        if (board == null) {
            board = BOARDS.get(RANDOM.nextInt(BOARDS.size()));
        }

        // Check if the board's catalog has already been requested
        if (!CACHE.containsKey(board) || CACHE.get(board).isEmpty()) {
            // Request board catalog, and get a list of threads on the board
            List<Integer> threadNumbers = new ArrayList<>();
            String url = "https://a.4cdn.org/" + board + "/catalog.json";
            String response = null;
            try {
                response = httpGet(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            JSONArray data = new JSONArray(response);

            // Get a list of threads in the data
            for (int i = 0; i < data.length(); i++) {
                JSONObject page = data.getJSONObject(i);
                JSONArray threads = page.getJSONArray("threads");
                for (int j = 0; j < threads.length(); j++) {
                    JSONObject thread = threads.getJSONObject(j);
                    threadNumbers.add(thread.getInt("no"));
                }
            }

            // Select a thread
            int thread = threadNumbers.get(RANDOM.nextInt(threadNumbers.size()));

            // Request the thread information, and get a list of images in that thread
            List<String> images = new ArrayList<>();
            url = "https://a.4cdn.org/" + board + "/thread/" + thread + ".json";
            try {
                response = httpGet(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            JSONObject postData = new JSONObject(response);
            JSONArray posts = postData.getJSONArray("posts");
            for (int i = 0; i < posts.length(); i++) {
                JSONObject post = posts.getJSONObject(i);
                if (post.has("tim") && post.has("ext")) {
                    images.add("https://i.4cdn.org/" + board + "/" + post.getLong("tim") + post.getString("ext") +
                            ' ' +
                            ("https://boards.4chan.org/" + board + "/thread/" + thread));
                }
            }

            // Save images to cache
            CACHE.put(board, images);
        }

        List<String> images = CACHE.get(board);
        String image = images.remove(RANDOM.nextInt(images.size()));
        String threadUrl = image.split(" ")[1];
        image = image.split(" ")[0];

        // Assemble and return the urls
        return new String[] { image, threadUrl };
    }

    public static String getPicSumImage() {
        try {
            // Create a URL object with the specified URL
            String url = "https://picsum.photos/1920/1080";
            
            // Create a new OkHttpClient and set it to not follow redirects

    
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = HTTP_CLIENT.newCall(request).execute();
    
            // Get the redirected URL from the "Location" header field
            String redirectedUrl = response.header("Location");
            return redirectedUrl;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    

    public static String getImgurImage() {
        String[] IMAGE_FORMATS = { "jpg", "png", "gif" };
        String imgurId = getRandomImgurId();
        String imageUrl = "https://i.imgur.com/" + imgurId + "." + IMAGE_FORMATS[RANDOM.nextInt(IMAGE_FORMATS.length)];
        try {
            URL url = new URL(imageUrl);
            Image image = ImageIO.read(url);
            if (image == null || image.getWidth(null) == 161 || image.getHeight(null) == 81) {
                // Failed to read the image or the image has invalid dimensions, try again with a different Imgur ID
                return getImgurImage();
            }
        } catch (IOException e) {
            // Failed to read the image, try again with a different Imgur ID
            return getImgurImage();
        }
        return imageUrl;
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
        String apiKey = Main.getEnv("GOOGLE_API_KEY");
        String cseId = Main.getEnv("GOOGLE_CX");
        Random rand = new Random();
        int start = rand.nextInt(5) + 1;
        // Encode the query string
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String url = String.format(
                "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&searchType=image&start=%d", apiKey,
                cseId, encodedQuery, start);
        String response = httpGet(url);
        JSONObject json = new JSONObject(response);
        JSONArray items = json.getJSONArray("items");
        int index = rand.nextInt(items.length());
        JSONObject randomImage = items.getJSONObject(index);
        return randomImage.getString("link");
    }

    private static String httpGet(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        if (!response.isSuccessful()) {
            // An error occurred, print the error message
            System.err.println("Error: " + response.body().string());
            return null;
        }
        return response.body().string();
    }

    public static String getTenor(String query) throws IOException {
        Request request = new Request.Builder()
                .url("https://tenor.googleapis.com/v2/search?key=" + Main.getEnv("TENOR_API") + "&q=" + URLEncoder.encode(query, "UTF-8") + "&limit=50")
                .build();
    
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
    
            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONArray resultsArray = jsonObject.getJSONArray("results");
            int randomIndex = new Random().nextInt(resultsArray.length());
            
            JSONObject resultObject = resultsArray.getJSONObject(randomIndex);
            
            // Check if "media_formats" and "gif" keys exist
            if (resultObject.has("media_formats") && resultObject.getJSONObject("media_formats").has("gif")) {
                String gifUrl = resultObject
                        .getJSONObject("media_formats")
                        .getJSONObject("gif").getString("url");
                
                return gifUrl;
            } else {
                throw new JSONException("Key 'media_formats' or 'gif' not found in JSONObject");
            }
        }
    }
    
    

}