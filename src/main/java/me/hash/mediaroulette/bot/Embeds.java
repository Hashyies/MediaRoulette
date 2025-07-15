package me.hash.mediaroulette.bot;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.utils.media.image_generation.ImageGenerator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Embeds {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static CompletableFuture<Message> sendImageEmbed(Interaction event, Map<String, String> map, boolean shouldContinue) {
        return processEmbed(event, map, shouldContinue, false);
    }

    public static void editImageEmbed(ButtonInteractionEvent event, Map<String, String> map) {
        processEmbed(event, map, true, true);
    }

    private static CompletableFuture<Message> processEmbed(Interaction event, Map<String, String> map,
                                                           boolean shouldContinue, boolean isEdit) {
        CompletableFuture<Message> future = new CompletableFuture<>();

        // Extract color asynchronously
        extractDominantColor(map).thenAccept(color -> {
            EmbedBuilder embed = createEmbed(event, map, color);
            List<Button> buttons = createButtons(shouldContinue);

            if ("create".equals(map.get("image_type"))) {
                handleGeneratedImage(event, embed, buttons, map, isEdit, future);
            } else if (isEdit) {
                editMessage((ButtonInteractionEvent) event, embed.build(), buttons);
                future.complete(null);
            } else {
                sendMessage(event, embed.build(), buttons, future);
            }
        }).exceptionally(throwable -> {
            // Fallback to default color on error
            EmbedBuilder embed = createEmbed(event, map, Color.CYAN);
            List<Button> buttons = createButtons(shouldContinue);

            if (isEdit) {
                editMessage((ButtonInteractionEvent) event, embed.build(), buttons);
                future.complete(null);
            } else {
                sendMessage(event, embed.build(), buttons, future);
            }
            return null;
        });

        return future;
    }

    private static void handleGeneratedImage(Interaction event, EmbedBuilder embed, List<Button> buttons,
                                             Map<String, String> map, boolean isEdit, CompletableFuture<Message> future) {
        User user = Main.userService.getOrCreateUser(event.getUser().getId());
        byte[] imageBytes = new ImageGenerator().generateImage(map.get("image_content"), user.getTheme());
        FileUpload file = FileUpload.fromData(imageBytes, "image.png");
        embed.setImage("attachment://image.png");

        if (isEdit) {
            ((ButtonInteractionEvent) event).editMessage(MessageEditData.fromEmbeds(embed.build()))
                    .setFiles(file)
                    .setComponents(ActionRow.of(buttons))
                    .queue();
            future.complete(null);
        } else {
            sendMessageWithFile(event, embed.build(), file, buttons, future);
        }
    }

    private static void sendMessage(Interaction event, MessageEmbed embed, List<Button> buttons,
                                    CompletableFuture<Message> future) {
        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IReplyCallback replyCallback) {
            if (replyCallback.isAcknowledged()) {
                // Already acknowledged/deferred, use hook to send followup
                replyCallback.getHook().sendMessageEmbeds(embed)
                        .addComponents(ActionRow.of(buttons))
                        .queue(future::complete, future::completeExceptionally);
            } else {
                // Not acknowledged yet, use reply
                replyCallback.replyEmbeds(embed)
                        .addComponents(ActionRow.of(buttons))
                        .queue(hook -> hook.retrieveOriginal().queue(future::complete), future::completeExceptionally);
            }
        }
    }

    private static void sendMessageWithFile(Interaction event, MessageEmbed embed, FileUpload file,
                                            List<Button> buttons, CompletableFuture<Message> future) {
        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IReplyCallback replyCallback) {
            if (replyCallback.isAcknowledged()) {
                // Already acknowledged/deferred, use hook to send followup
                replyCallback.getHook().sendMessageEmbeds(embed)
                        .addFiles(file)
                        .addComponents(ActionRow.of(buttons))
                        .queue(future::complete, future::completeExceptionally);
            } else {
                // Not acknowledged yet, use reply
                replyCallback.replyEmbeds(embed)
                        .addFiles(file)
                        .addComponents(ActionRow.of(buttons))
                        .queue(hook -> hook.retrieveOriginal().queue(future::complete), future::completeExceptionally);
            }
        }
    }

    private static void editMessage(ButtonInteractionEvent event, MessageEmbed embed, List<Button> buttons) {
        event.editMessage(MessageEditData.fromEmbeds(embed))
                .setComponents(ActionRow.of(buttons))
                .queue();
    }

    private static List<Button> createButtons(boolean shouldContinue) {
        String suffix = shouldContinue ? ":continue" : "";
        List<Button> buttons = List.of(
                Button.success("safe" + suffix, "Safe").withEmoji(Emoji.fromUnicode("✔️")),
                Button.primary("favorite", "Favorite").withEmoji(Emoji.fromUnicode("⭐")),
                Button.danger("nsfw" + suffix, "NSFW").withEmoji(Emoji.fromUnicode("🔞"))
        );

        return shouldContinue ?
                List.of(buttons.get(0), buttons.get(1), buttons.get(2),
                        Button.secondary("exit", "Exit").withEmoji(Emoji.fromUnicode("❌"))) :
                buttons;
    }

    // FIXED: Handle button interactions properly without double acknowledgment
    public static void disableButton(ButtonInteractionEvent event, String buttonId) {
        // Check if the interaction has already been acknowledged
        if (!event.isAcknowledged()) {
            // If not acknowledged, defer the edit first
            event.deferEdit().queue(success -> {
                updateButtonComponents(event, buttonId);
            }, failure -> {
                System.err.println("Failed to defer edit: " + failure.getMessage());
            });
        } else {
            // If already acknowledged, use the hook to edit
            updateButtonComponents(event, buttonId);
        }
    }

    // FIXED: Handle button interactions properly without double acknowledgment
    public static void disableAllButtons(ButtonInteractionEvent event) {
        // Check if the interaction has already been acknowledged
        if (!event.isAcknowledged()) {
            // If not acknowledged, defer the edit first
            event.deferEdit().queue(success -> {
                updateAllButtonComponents(event);
            }, failure -> {
                System.err.println("Failed to defer edit: " + failure.getMessage());
            });
        } else {
            // If already acknowledged, use the hook to edit
            updateAllButtonComponents(event);
        }
    }

    // Helper method to update specific button component
    private static void updateButtonComponents(ButtonInteractionEvent event, String buttonId) {
        List<ActionRow> updatedRows = event.getMessage().getActionRows().stream()
                .map(row -> ActionRow.of(row.getComponents().stream()
                        .map(comp -> comp instanceof Button btn && buttonId.equals(btn.getId()) ?
                                btn.asDisabled() : comp)
                        .toList()))
                .toList();

        if (event.isAcknowledged()) {
            // Use hook if already acknowledged
            event.getHook().editOriginalComponents(updatedRows).queue();
        } else {
            // This shouldn't happen if we deferred properly, but just in case
            event.editComponents(updatedRows).queue();
        }
    }

    // Helper method to update all button components
    private static void updateAllButtonComponents(ButtonInteractionEvent event) {
        List<ActionRow> disabledRows = event.getMessage().getActionRows().stream()
                .map(ActionRow::asDisabled)
                .toList();

        if (event.isAcknowledged()) {
            // Use hook if already acknowledged
            event.getHook().editOriginalComponents(disabledRows).queue();
        } else {
            // This shouldn't happen if we deferred properly, but just in case
            event.editComponents(disabledRows).queue();
        }
    }

    private static EmbedBuilder createEmbed(Interaction event, Map<String, String> map, Color color) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(map.get("title"))
                .setDescription(map.get("description"))
                .setColor(color)
                .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());

        String image = map.get("image");
        if (!"none".equals(image)) {
            embed.setImage(image);
            if (map.containsKey("link")) {
                embed.setUrl(map.get("link"));
            } else if (!image.startsWith("attachment://")) {
                embed.setUrl(image);
            }
        }

        return embed;
    }

    private static CompletableFuture<Color> extractDominantColor(Map<String, String> map) {
        String imageUrl = map.get("image");
        if ("none".equals(imageUrl) || imageUrl.startsWith("attachment://")) {
            return CompletableFuture.completedFuture(Color.CYAN);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Handle different media types
                if (isVideoUrl(imageUrl)) {
                    return extractColorFromVideo(imageUrl);
                } else if (isGifUrl(imageUrl)) {
                    return extractColorFromGif(imageUrl);
                } else {
                    return extractColorFromImage(imageUrl);
                }
            } catch (Exception e) {
                System.err.println("Failed to extract color from: " + imageUrl + " - " + e.getMessage());
                return Color.CYAN;
            }
        });
    }

    private static boolean isVideoUrl(String url) {
        return url.matches(".*\\.(mp4|webm|mov|avi)$") || url.contains("redgifs.com");
    }

    private static boolean isGifUrl(String url) {
        return url.matches(".*\\.gif$");
    }

    private static Color extractColorFromVideo(String url) throws Exception {
        // For video URLs, try to get thumbnail or first frame
        // This is a simplified approach - you might want to use FFmpeg for better results
        if (url.contains("redgifs.com")) {
            // RedGifs specific handling - try to get preview image
            String previewUrl = url.replace(".com/watch/", ".com/ifr/") + "-preview.jpg";
            return extractColorFromImage(previewUrl);
        }
        return Color.CYAN; // Fallback for unsupported video formats
    }

    private static Color extractColorFromGif(String url) throws Exception {
        // For GIFs, extract color from first frame
        return extractColorFromImage(url);
    }

    private static Color extractColorFromImage(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch image: " + response.statusCode());
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
        if (image == null) {
            throw new IOException("Could not decode image");
        }

        return getDominantColor(image);
    }

    private static Color getDominantColor(BufferedImage image) {
        // Scale down image for faster processing
        int width = Math.min(image.getWidth(), 100);
        int height = Math.min(image.getHeight(), 100);

        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();

        long redSum = 0, greenSum = 0, blueSum = 0;
        int pixelCount = 0;

        // Sample pixels and calculate average
        for (int x = 0; x < width; x += 2) {
            for (int y = 0; y < height; y += 2) {
                int rgb = scaled.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Skip very dark or very light pixels for better color representation
                int brightness = (r + g + b) / 3;
                if (brightness > 30 && brightness < 225) {
                    redSum += r;
                    greenSum += g;
                    blueSum += b;
                    pixelCount++;
                }
            }
        }

        if (pixelCount == 0) {
            return Color.CYAN;
        }

        int avgRed = (int) (redSum / pixelCount);
        int avgGreen = (int) (greenSum / pixelCount);
        int avgBlue = (int) (blueSum / pixelCount);

        // Enhance saturation for better visual appeal
        return enhanceSaturation(new Color(avgRed, avgGreen, avgBlue));
    }

    private static Color enhanceSaturation(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        // Boost saturation and brightness slightly
        float saturation = Math.min(1.0f, hsb[1] * 1.3f);
        float brightness = Math.min(1.0f, hsb[2] * 1.1f);
        return Color.getHSBColor(hsb[0], saturation, brightness);
    }
}