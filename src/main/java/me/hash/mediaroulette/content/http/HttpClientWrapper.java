package me.hash.mediaroulette.content.http;

import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpClientWrapper {
    private final OkHttpClient client;

    public HttpClientWrapper() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .followRedirects(true)
                .build();
    }

    public String get(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed: " + response.code());
            }
            return response.body().string();
        }
    }

    public Response getResponse(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        return client.newCall(request).execute();
    }

    public OkHttpClient getClient() {
        return client;
    }
}