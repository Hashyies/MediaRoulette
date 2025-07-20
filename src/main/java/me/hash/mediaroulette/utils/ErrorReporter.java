package me.hash.mediaroulette.utils;

import me.hash.mediaroulette.Main;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for reporting errors to a Discord webhook
 */
public class ErrorReporter {
    private static WebhookClient webhookClient;
    private static boolean initialized = false;
    
    private static void initialize() {
        if (!initialized) {
            String webhookUrl = Main.getEnv("ERROR_WEBHOOK");
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                webhookClient = WebhookClient.withUrl(webhookUrl);
            }
            initialized = true;
        }
    }
    
    /**
     * Report a failed subreddit error
     */
    public static void reportFailedSubreddit(String subreddit, String error, String userId) {
        reportError("Failed Subreddit", 
                   String.format("Subreddit '%s' failed validation or access", subreddit),
                   error, 
                   "reddit", 
                   userId);
    }
    
    /**
     * Report a failed 4chan board error
     */
    public static void reportFailed4ChanBoard(String board, String error, String userId) {
        reportError("Failed 4Chan Board", 
                   String.format("4Chan board '%s' failed validation or access", board),
                   error, 
                   "4chan", 
                   userId);
    }
    
    /**
     * Report a general provider error
     */
    public static void reportProviderError(String provider, String operation, String error, String userId) {
        reportError("Provider Error", 
                   String.format("%s provider failed during %s", provider, operation),
                   error, 
                   provider, 
                   userId);
    }
    
    /**
     * Generic error reporting method
     */
    public static void reportError(String title, String description, String errorDetails, String source, String userId) {
        initialize();
        
        if (webhookClient == null) {
            // Fallback to console logging if webhook is not configured
            System.err.println(String.format("[ERROR REPORT] %s - %s: %s (User: %s, Source: %s)", 
                title, description, errorDetails, userId != null ? userId : "unknown", source));
            return;
        }
        
        try {
            WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setTitle(new WebhookEmbed.EmbedTitle("🚨 " + title, null))
                .setDescription(description)
                .addField(new WebhookEmbed.EmbedField(true, "Error Details", 
                    errorDetails.length() > 1000 ? errorDetails.substring(0, 1000) + "..." : errorDetails))
                .addField(new WebhookEmbed.EmbedField(true, "Source", source))
                .addField(new WebhookEmbed.EmbedField(true, "User ID", userId != null ? userId : "Unknown"))
                .setTimestamp(Instant.now())
                .setColor(0xFF0000); // Red color for errors
            
            CompletableFuture<Void> future = webhookClient.send(embedBuilder.build())
                .thenAccept(message -> {
                    // Success - no action needed
                })
                .exceptionally(throwable -> {
                    System.err.println("Failed to send error report to webhook: " + throwable.getMessage());
                    return null;
                });
                
        } catch (Exception e) {
            System.err.println("Error while trying to report error: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup method to close webhook client
     */
    public static void cleanup() {
        if (webhookClient != null) {
            webhookClient.close();
        }
    }
}