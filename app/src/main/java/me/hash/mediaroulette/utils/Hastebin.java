package me.hash.mediaroulette.utils;

import okhttp3.*;

import java.io.IOException;

import org.json.JSONObject;

import me.hash.mediaroulette.Main;

public class Hastebin {
    public static String createPaste(String content) throws IOException {
        OkHttpClient client = new OkHttpClient();
    
        RequestBody body = RequestBody.create(content, MediaType.parse("text/plain"));
        Request request = new Request.Builder()
                .url("https://hastebin.com/documents")
                .post(body)
                .addHeader("Authorization", "Bearer " + Main.getEnv("HASTEBIN_TOKEN"))
                .build();
    
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            System.out.println(responseBody);
            JSONObject json = new JSONObject(responseBody);
    
            if (json.has("key")) {
                return json.getString("key");
            } else {
                throw new IOException("Failed to create paste");
            }
        }
    }
    
    public static String getPaste(String pasteId) throws IOException {
        OkHttpClient client = new OkHttpClient();
    
        Request request = new Request.Builder()
                .url("https://hastebin.com/raw/" + pasteId)
                .get()
                .addHeader("Authorization", "Bearer " + Main.getEnv("HASTEBIN_TOKEN"))
                .build();
    
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
    
}
