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
                .addHeader("Content-Type", "text/plain")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            System.out.println("Hastebin response: " + responseBody);
            JSONObject json = new JSONObject(responseBody);

            if (json.has("key")) {
                return json.getString("key");
            } else {
                throw new IOException("Failed to create paste - no key in response: " + responseBody);
            }
        }
    }

    public static String getPaste(String pasteId) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Fixed: Use the correct endpoint for getting documents
        Request request = new Request.Builder()
                .url("https://hastebin.com/documents/" + pasteId)
                .get()
                .addHeader("Authorization", "Bearer " + Main.getEnv("HASTEBIN_TOKEN"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();

            // The response for getting a document is JSON with a "data" field
            JSONObject json = new JSONObject(responseBody);
            if (json.has("data")) {
                return json.getString("data");
            } else {
                throw new IOException("Failed to get paste - no data in response: " + responseBody);
            }
        }
    }

    // Alternative method to get raw content (if needed)
    public static String getPasteRaw(String pasteId) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://hastebin.com/raw/" + pasteId)
                .get()
                .addHeader("Authorization", "Bearer " + Main.getEnv("HASTEBIN_TOKEN"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code() + " - " + response.message());
            }

            return response.body().string();
        }
    }
}