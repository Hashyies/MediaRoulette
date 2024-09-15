package me.hash.mediaroulette.utils.random.reddit;

import me.hash.mediaroulette.Main;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RedditClient {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded");
    private static String accessToken = null;
    private static long accessTokenExpirationTime = 0;

    public static OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES)) // connection pooling
            .build();

    public String getAccessToken() throws IOException {
        if (accessToken == null || System.currentTimeMillis() > accessTokenExpirationTime) {
            return fetchAccessToken();
        }
        return accessToken;
    }

    private String fetchAccessToken() throws IOException {
        String authString = Main.getEnv("REDDIT_CLIENT_ID") + ":" + Main.getEnv("REDDIT_CLIENT_SECRET");
        String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());

        String url = "https://www.reddit.com/api/v1/access_token";
        RequestBody body = RequestBody.create(
                "grant_type=password&username=" + Main.getEnv("REDDIT_USERNAME") +
                        "&password=" + Main.getEnv("REDDIT_PASSWORD"), MEDIA_TYPE
        );
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Basic " + encodedAuthString)
                .addHeader("User-Agent", "MediaRoulette/0.1 by pgmmestar")
                .build();

        Response response = HTTP_CLIENT.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Failed to retrieve access token");
        }

        String responseBody = response.body().string();
        JSONObject json = new JSONObject(responseBody);
        accessToken = json.getString("access_token");
        accessTokenExpirationTime = System.currentTimeMillis() + json.getLong("expires_in") * 1000;

        return accessToken;
    }

    public CompletableFuture<Response> sendGetRequestAsync(String url, String token) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("User-Agent", "MediaRoulette/0.1 by pgmmestar")
                .build();

        CompletableFuture<Response> future = new CompletableFuture<>();
        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                future.complete(response);
            }
        });
        return future;
    }
}
