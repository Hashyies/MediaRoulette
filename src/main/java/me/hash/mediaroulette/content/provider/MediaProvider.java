package me.hash.mediaroulette.content.provider;

import me.hash.mediaroulette.content.http.HttpClientWrapper;
import me.hash.mediaroulette.model.content.MediaResult;
import java.io.IOException;

public interface MediaProvider {
    MediaResult getRandomMedia(String query) throws IOException, HttpClientWrapper.RateLimitException, InterruptedException;
    boolean supportsQuery();
    String getProviderName();
}