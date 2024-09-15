package me.hash.mediaroulette.utils.random;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONObject;
import org.json.JSONArray;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RandomText {

    private static final String BASE_URL = "https://api.urbandictionary.com/v0";
    private static final OkHttpClient client = new OkHttpClient();

    public static Map<String, String> getRandomUrbanWord() throws IOException {
        return getRandomUrbanWord(null);
    }

    public static Map<String, String> getRandomUrbanWord(String meaning) throws IOException {
        Request request;
        if (meaning != null) {
            request = new Request.Builder()
                    .url(BASE_URL + "/define?term=" + meaning)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(BASE_URL + "/random")
                    .build();
        }

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);

            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONArray list = jsonObject.getJSONArray("list");
            if (list.length() == 0) {
                return Map.of("error", "No word found with the given meaning.");
            }

            JSONObject randomWord;
            if (meaning != null) {
                randomWord = list.getJSONObject(0);
            } else {
                randomWord = list.getJSONObject(new Random().nextInt(list.length()));
            }

            HashMap<String, String> map = new HashMap<>();
            
            map.put("description", "Word: " + randomWord.getString("word") + "\n"
                + "Meaning: " + randomWord.getString("definition") + "\n"
                + "Submitted by: " + randomWord.getString("author") + "\n"
                + "Date: " + randomWord.getString("written_on"));

            map.put("image", "attachment://image.png");
            map.put("image_type", "create");
            map.put("image_content", randomWord.getString("word"));
            map.put("title", "Here is your random Urban Dictionary word!");
            return map;
        }
    }

    public static byte[] generateImage(String text) {
        // Increase the resolution by 2.5 times
        int width = (int) (500 * 2.5);
        int height = (int) (300 * 2.5);
    
        // Create a new dark image
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
    
        // Enable anti-aliasing and rendering optimizations
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
    
        // Set the background color to dark
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, img.getWidth(), img.getHeight());
    
        // Set the font and color
        Font font = new Font("Arial", Font.PLAIN, (int) (40 * 2.5)); // Increase the font size by 2.5 times
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
    
        // Calculate the size of the text
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
    
        // Calculate the position to center the text
        int x = (img.getWidth() - textWidth) / 2;
        int y = ((img.getHeight() - textHeight) / 2) + fm.getAscent();
    
        // Ensure there is always a gap between the sides
        int padding = 10; // Adjust as needed
        if (x < padding) {
            x = padding;
        }
        if (y < padding) {
            y = padding;
        }
    
        // Draw the text
        g2d.drawString(text, x, y);
    
        // Dispose the Graphics2D object
        g2d.dispose();
    
        // Convert the image to bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }     
    
}