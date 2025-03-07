package me.hash.mediaroulette.content;

import me.hash.mediaroulette.content.providers.*;

public class ContentProviderFactory {
    public static ContentProvider getProvider(String source) {
        switch (source.toLowerCase()) {
            case "4chan":
                return new FourChanContentProvider();
            case "picsum":
                return new PicsumContentProvider();
            case "imgur":
                return new ImgurContentProvider();
            case "rule34":
                return new Rule34ContentProvider();
            case "google":
                return new GoogleContentProvider();
            case "tenor":
                return new TenorContentProvider();
            case "tmdb_movie":
                return new TMDBContentProvider("movie");
            case "tmdb_tv":
                return new TMDBContentProvider("tv");
            case "youtube":
                return new YouTubeContentProvider("");
            case "youtube_shorts":
                return new YouTubeContentProvider("shorts");
            default:
                throw new IllegalArgumentException("Unknown content source: " + source);
        }
    }
}
