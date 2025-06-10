package me.hash.mediaroulette.content.provider;

import me.hash.mediaroulette.model.content.MediaResult;
import java.io.IOException;

public interface MediaProvider {
    MediaResult getRandomMedia(String query) throws IOException;
    boolean supportsQuery();
    String getProviderName();
}