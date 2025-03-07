package me.hash.mediaroulette.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Random;

public class ImageUtils {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final String IMGUR_ID_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int[] IMGUR_ID_LENGTH_RANGE = {5, 6};
    private static final Random RANDOM = new Random();

    public static String httpGet(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if(response.body() != null) {
                return response.body().string();
            }
            throw new IOException("No response body");
        }
    }

    public static String getRandomImgurId() {
        int length = RANDOM.nextInt(IMGUR_ID_LENGTH_RANGE[1] - IMGUR_ID_LENGTH_RANGE[0] + 1) + IMGUR_ID_LENGTH_RANGE[0];
        StringBuilder idBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int pos = RANDOM.nextInt(IMGUR_ID_CHARACTERS.length());
            idBuilder.append(IMGUR_ID_CHARACTERS.charAt(pos));
        }
        return idBuilder.toString();
    }
}
