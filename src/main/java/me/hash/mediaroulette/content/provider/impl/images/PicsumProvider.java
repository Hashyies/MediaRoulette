// PicsumProvider.java
package me.hash.mediaroulette.content.provider.impl.images;

import me.hash.mediaroulette.model.content.MediaResult;
import me.hash.mediaroulette.model.content.MediaSource;
import me.hash.mediaroulette.content.provider.MediaProvider;
import me.hash.mediaroulette.content.http.HttpClientWrapper;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class PicsumProvider implements MediaProvider {
    private final HttpClientWrapper httpClient;

    public PicsumProvider(HttpClientWrapper httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public MediaResult getRandomMedia(String query) throws IOException {
        String url = "https://picsum.photos/1920/1080";

        Request request = new Request.Builder().url(url).build();
        try (Response response = httpClient.getClient().newCall(request).execute()) {
            String redirectUrl = response.header("Location");
            if (redirectUrl == null) {
                throw new IOException("No redirect URL found from Picsum");
            }

            return new MediaResult(
                    redirectUrl,
                    "Here is your random Picsum image!",
                    "🌐 Source: Picsum\n",
                    MediaSource.PICSUM
            );
        }
    }

    @Override
    public boolean supportsQuery() {
        return false;
    }

    @Override
    public String getProviderName() {
        return "Picsum";
    }
}