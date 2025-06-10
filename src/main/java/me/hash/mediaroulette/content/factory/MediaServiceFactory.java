package me.hash.mediaroulette.content.factory;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.content.http.HttpClientWrapper;
import me.hash.mediaroulette.content.provider.MediaProvider;
import me.hash.mediaroulette.content.provider.impl.gifs.TenorProvider;
import me.hash.mediaroulette.content.provider.impl.images.*;
import me.hash.mediaroulette.content.provider.impl.videos.TMDBMovieProvider;
import me.hash.mediaroulette.content.provider.impl.videos.TMDBTvProvider;
import me.hash.mediaroulette.content.provider.impl.videos.YouTubeProvider;
import me.hash.mediaroulette.content.provider.impl.videos.YouTubeShortsProvider;
import me.hash.mediaroulette.content.reddit.RedditClient;
import me.hash.mediaroulette.content.reddit.SubredditManager;

public class MediaServiceFactory {
    private final HttpClientWrapper httpClient;
    private final RedditClient redditClient;
    private final SubredditManager subredditManager;

    public MediaServiceFactory() {
        this.httpClient = new HttpClientWrapper();
        this.redditClient = new RedditClient();
        this.subredditManager = new SubredditManager(redditClient);
    }

    public MediaProvider createFourChanProvider() {
        return new FourChanProvider(httpClient);
    }

    public MediaProvider createPicsumProvider() {
        return new PicsumProvider(httpClient);
    }

    public MediaProvider createImgurProvider() {
        return new ImgurProvider();
    }

    public MediaProvider createGoogleProvider() {
        return new GoogleProvider(httpClient, Main.getEnv("GOOGLE_API_KEY"), Main.getEnv("GOOGLE_CX"));
    }

    public MediaProvider createTenorProvider() {
        return new TenorProvider(httpClient, Main.getEnv("TENOR_API"));
    }

    public MediaProvider createRedditProvider() {
        return new RedditProvider(redditClient, subredditManager);
    }

    public MediaProvider createRule34Provider() {
        return new Rule34Provider(httpClient);
    }

    public MediaProvider createTMDBTvProvider() {
        return new TMDBTvProvider(httpClient, Main.getEnv("TMDB_API"));
    }

    public MediaProvider createTMDBMovieProvider() {
        return new TMDBMovieProvider(httpClient, Main.getEnv("TMDB_API"));
    }

    public MediaProvider createYouTubeProvider() {
        return new YouTubeProvider(httpClient, Main.getEnv("GOOGLE_API_KEY"));
    }

    public MediaProvider createYouTubeShortsProvider() {
        return new YouTubeShortsProvider(httpClient, Main.getEnv("GOOGLE_API_KEY"));
    }
}