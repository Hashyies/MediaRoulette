package me.hash.mediaroulette.utils.media.ffmpeg.config;

import me.hash.mediaroulette.utils.media.ffmpeg.utils.FileManager;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Configuration class for FFmpeg operations
 */
public class FFmpegConfig {
    // Timeout configurations
    private final int defaultTimeoutSeconds;
    private final int gifCreationTimeoutSeconds;
    private final int thumbnailTimeoutSeconds;
    private final int videoInfoTimeoutSeconds;
    
    // Quality settings
    private final int defaultGifWidth;
    private final int defaultGifHeight;
    private final int defaultGifFps;
    private final double maxGifDuration;
    
    // Discord file size limits (in bytes)
    private final long maxDiscordFileSize;
    private final long maxDiscordFileSizePremium;
    
    // File management
    private final String tempDirectory;
    private final FileManager fileManager;
    
    // HTTP client for URL operations
    private final OkHttpClient httpClient;

    public FFmpegConfig() {
        this.defaultTimeoutSeconds = 30;
        this.gifCreationTimeoutSeconds = 120; // Increased for GIF creation
        this.thumbnailTimeoutSeconds = 30;
        this.videoInfoTimeoutSeconds = 15;
        
        this.defaultGifWidth = 480;
        this.defaultGifHeight = 270;
        this.defaultGifFps = 12; // Reduced from 15 to 12 for smaller files
        this.maxGifDuration = 30.0; // Max 30 seconds to stay under Discord limits
        
        // Discord file size limits: 25MB for regular users, 500MB for premium
        this.maxDiscordFileSize = 25 * 1024 * 1024; // 25MB
        this.maxDiscordFileSizePremium = 500 * 1024 * 1024; // 500MB
        
        this.tempDirectory = "temp";
        this.fileManager = new FileManager(tempDirectory);
        
        this.httpClient = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    // Getters
    public int getDefaultTimeoutSeconds() { return defaultTimeoutSeconds; }
    public int getGifCreationTimeoutSeconds() { return gifCreationTimeoutSeconds; }
    public int getThumbnailTimeoutSeconds() { return thumbnailTimeoutSeconds; }
    public int getVideoInfoTimeoutSeconds() { return videoInfoTimeoutSeconds; }
    
    public int getDefaultGifWidth() { return defaultGifWidth; }
    public int getDefaultGifHeight() { return defaultGifHeight; }
    public int getDefaultGifFps() { return defaultGifFps; }
    public double getMaxGifDuration() { return maxGifDuration; }
    
    public long getMaxDiscordFileSize() { return maxDiscordFileSize; }
    public long getMaxDiscordFileSizePremium() { return maxDiscordFileSizePremium; }
    
    public String getTempDirectory() { return tempDirectory; }
    public FileManager getFileManager() { return fileManager; }
    public OkHttpClient getHttpClient() { return httpClient; }
}