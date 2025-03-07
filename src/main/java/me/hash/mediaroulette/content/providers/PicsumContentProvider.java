package me.hash.mediaroulette.content.providers;

import me.hash.mediaroulette.content.ContentInfo;
import me.hash.mediaroulette.content.ContentProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class PicsumContentProvider implements ContentProvider {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .followRedirects(false)
            .build();

    @Override
    public ContentInfo getRandomContent() throws IOException {
        String url = "https://picsum.photos/1920/1080";
        Request request = new Request.Builder().url(url).build();
        Response response = HTTP_CLIENT.newCall(request).execute();

        String imageUrl = response.header("Location");
        String title = "Here is your random Picsum image!";
        String description = "🌐 Source: Picsum";
        return new ContentInfo(title, description, imageUrl);
    }
}
