package me.hash.mediaroulette.utils.media.ffmpeg;

import me.hash.mediaroulette.utils.media.ffmpeg.config.FFmpegConfig;
import me.hash.mediaroulette.utils.media.ffmpeg.processors.VideoProcessor;
import me.hash.mediaroulette.utils.media.ffmpeg.processors.ThumbnailProcessor;
import me.hash.mediaroulette.utils.media.ffmpeg.processors.GifProcessor;
import me.hash.mediaroulette.utils.media.ffmpeg.resolvers.UrlResolverFactory;
import me.hash.mediaroulette.utils.media.ffmpeg.models.VideoInfo;
import me.hash.mediaroulette.utils.media.FFmpegDownloader;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main service class for FFmpeg operations.
 * Provides a clean, object-oriented interface for video processing tasks.
 */
public class FFmpegService {
    private final FFmpegConfig config;
    private final VideoProcessor videoProcessor;
    private final ThumbnailProcessor thumbnailProcessor;
    private final GifProcessor gifProcessor;
    private final UrlResolverFactory urlResolverFactory;

    public FFmpegService() {
        this.config = new FFmpegConfig();
        this.videoProcessor = new VideoProcessor(config);
        this.thumbnailProcessor = new ThumbnailProcessor(config);
        this.gifProcessor = new GifProcessor(config);
        this.urlResolverFactory = new UrlResolverFactory();
    }

    public FFmpegService(FFmpegConfig config) {
        this.config = config;
        this.videoProcessor = new VideoProcessor(config);
        this.thumbnailProcessor = new ThumbnailProcessor(config);
        this.gifProcessor = new GifProcessor(config);
        this.urlResolverFactory = new UrlResolverFactory();
    }

    /**
     * Resolves a video URL to its direct media URL
     */
    public CompletableFuture<String> resolveVideoUrl(String url) {
        return urlResolverFactory.getResolver(url).resolve(url);
    }

    /**
     * Gets video information using ffprobe
     */
    public CompletableFuture<VideoInfo> getVideoInfo(String videoUrl) {
        return resolveVideoUrl(videoUrl)
                .thenCompose(videoProcessor::getVideoInfo);
    }

    /**
     * Extracts a single thumbnail from a video at the specified timestamp
     */
    public CompletableFuture<BufferedImage> extractThumbnail(String videoUrl, double timestampSeconds) {
        return resolveVideoUrl(videoUrl)
                .thenCompose(resolvedUrl -> thumbnailProcessor.extractThumbnail(resolvedUrl, timestampSeconds));
    }

    /**
     * Extracts multiple thumbnails from a video at specified timestamps
     */
    public CompletableFuture<List<BufferedImage>> extractMultipleThumbnails(String videoUrl, double[] timestamps) {
        return resolveVideoUrl(videoUrl)
                .thenCompose(resolvedUrl -> thumbnailProcessor.extractMultipleThumbnails(resolvedUrl, timestamps));
    }

    /**
     * Extracts the dominant color from a video by analyzing multiple frames
     */
    public CompletableFuture<Color> extractDominantColor(String videoUrl) {
        return resolveVideoUrl(videoUrl)
                .thenCompose(resolvedUrl -> getVideoInfo(resolvedUrl)
                        .thenCompose(videoInfo -> thumbnailProcessor.extractDominantColor(resolvedUrl, videoInfo)));
    }

    /**
     * Creates an optimized GIF from a video with smart parameters
     */
    public CompletableFuture<Path> createSmartGif(String videoUrl) {
        return resolveVideoUrl(videoUrl)
                .thenCompose(resolvedUrl -> getVideoInfo(resolvedUrl)
                        .thenCompose(videoInfo -> gifProcessor.createSmartGif(resolvedUrl, videoInfo)));
    }

    /**
     * Creates a GIF with custom parameters
     */
    public CompletableFuture<Path> createGif(String videoUrl, double startTime, double duration, int width, int height) {
        return resolveVideoUrl(videoUrl)
                .thenCompose(resolvedUrl -> gifProcessor.createGif(resolvedUrl, startTime, duration, width, height));
    }

    /**
     * Creates a FileUpload for Discord from a video GIF with size optimization
     */
    public CompletableFuture<net.dv8tion.jda.api.utils.FileUpload> createGifUpload(String videoUrl) {
        return resolveVideoUrl(videoUrl)
                .thenCompose(resolvedUrl -> getVideoInfo(resolvedUrl)
                        .thenCompose(videoInfo -> gifProcessor.createDiscordOptimizedGif(resolvedUrl, videoInfo))
                        .thenApply(gifProcessor::createFileUpload));
    }

    /**
     * Creates a video preview GIF (30 seconds, optimized size)
     */
    public CompletableFuture<Path> createVideoPreviewGif(String videoUrl) {
        return resolveVideoUrl(videoUrl)
                .thenCompose(resolvedUrl -> gifProcessor.createVideoPreviewGif(resolvedUrl));
    }

    /**
     * Checks if a URL is a video URL
     */
    public boolean isVideoUrl(String url) {
        return urlResolverFactory.isVideoUrl(url);
    }

    /**
     * Checks if a URL should be converted to GIF
     */
    public boolean shouldConvertToGif(String url) {
        return urlResolverFactory.shouldConvertToGif(url);
    }

    /**
     * Gets a preview URL for a video (thumbnail image)
     */
    public String getVideoPreviewUrl(String videoUrl) {
        return urlResolverFactory.getVideoPreviewUrl(videoUrl);
    }

    /**
     * Cleans up temporary files
     */
    public void cleanupTempFiles() {
        config.getFileManager().cleanupTempFiles();
    }

    /**
     * Checks if FFmpeg is ready for use
     */
    public CompletableFuture<Boolean> isReady() {
        return FFmpegDownloader.getFFmpegPath()
                .thenCompose(ffmpegPath -> FFmpegDownloader.getFFprobePath()
                        .thenApply(ffprobePath -> config.getFileManager().pathExists(ffmpegPath) && 
                                                 config.getFileManager().pathExists(ffprobePath)));
    }
}