package me.hash.mediaroulette.bot;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.utils.media.image_generation.ImageGenerator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

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

public class LoadingEmbeds {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static CompletableFuture<Message> editLoadingToImageEmbed(net.dv8tion.jda.api.interactions.InteractionHook hook, Map<String, String> map, boolean shouldContinue) {
        CompletableFuture<Message> future = new CompletableFuture<>();

        // Extract color asynchronously
        extractDominantColor(map).thenAccept(color -> {
            EmbedBuilder embed = createEmbedFromHook(hook, map, color);
            List<Button> buttons = createButtons(shouldContinue);

            if ("create".equals(map.get("image_type"))) {
                handleGeneratedImageEdit(hook, embed, buttons, map, future);
            } else {
                editMessageFromHook(hook, embed.build(), buttons, future);
            }
        }).exceptionally(throwable -> {
            // Fallback to default color on error
            EmbedBuilder embed = createEmbedFromHook(hook, map, Color.CYAN);
            List<Button> buttons = createButtons(shouldContinue);

            if ("create".equals(map.get("image_type"))) {
                handleGeneratedImageEdit(hook, embed, buttons, map, future);
            } else {
                editMessageFromHook(hook, embed.build(), buttons, future);
            }
            return null;
        });

        return future;
    }

    public static void editLoadingToImageEmbedFromHook(net.dv8tion.jda.api.interactions.InteractionHook hook, Map<String, String> map, boolean shouldContinue) {
        // Extract color asynchronously
        extractDominantColor(map).thenAccept(color -> {
            EmbedBuilder embed = createEmbedFromHook(hook, map, color);
            List<Button> buttons = createButtons(shouldContinue);

            if ("create".equals(map.get("image_type"))) {
                handleGeneratedImageEditFromHook(hook, embed, buttons, map);
            } else {
                editMessageFromHookVoid(hook, embed.build(), buttons);
            }
        }).exceptionally(throwable -> {
            // Fallback to default color on error
            EmbedBuilder embed = createEmbedFromHook(hook, map, Color.CYAN);
            List<Button> buttons = createButtons(shouldContinue);

            if ("create".equals(map.get("image_type"))) {
                handleGeneratedImageEditFromHook(hook, embed, buttons, map);
            } else {
                editMessageFromHookVoid(hook, embed.build(), buttons);
            }
            return null;
        });
    }

    private static EmbedBuilder createEmbedFromHook(net.dv8tion.jda.api.interactions.InteractionHook hook, Map<String, String> map, Color color) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(map.get("title"))
                .setDescription(map.get("description"))
                .setColor(color)
                .setAuthor(hook.getInteraction().getUser().getName(), null, hook.getInteraction().getUser().getEffectiveAvatarUrl());

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

    private static void handleGeneratedImageEdit(net.dv8tion.jda.api.interactions.InteractionHook hook, EmbedBuilder embed, List<Button> buttons,
                                                 Map<String, String> map, CompletableFuture<Message> future) {
        User user = Main.userService.getOrCreateUser(hook.getInteraction().getUser().getId());
        byte[] imageBytes = new ImageGenerator().generateImage(map.get("image_content"), user.getTheme());
        FileUpload file = FileUpload.fromData(imageBytes, "image.png");
        embed.setImage("attachment://image.png");

        hook.editOriginalEmbeds(embed.build())
                .setFiles(file)
                .setComponents(ActionRow.of(buttons))
                .queue(future::complete, future::completeExceptionally);
    }

    private static void editMessageFromHook(net.dv8tion.jda.api.interactions.InteractionHook hook, MessageEmbed embed, List<Button> buttons,
                                            CompletableFuture<Message> future) {
        hook.editOriginalEmbeds(embed)
                .setComponents(ActionRow.of(buttons))
                .queue(future::complete, future::completeExceptionally);
    }

    private static void handleGeneratedImageEditFromHook(net.dv8tion.jda.api.interactions.InteractionHook hook, EmbedBuilder embed, List<Button> buttons,
                                                         Map<String, String> map) {
        User user = Main.userService.getOrCreateUser(hook.getInteraction().getUser().getId());
        byte[] imageBytes = new ImageGenerator().generateImage(map.get("image_content"), user.getTheme());
        FileUpload file = FileUpload.fromData(imageBytes, "image.png");
        embed.setImage("attachment://image.png");

        hook.editOriginalEmbeds(embed.build())
                .setFiles(file)
                .setComponents(ActionRow.of(buttons))
                .queue();
    }

    private static void editMessageFromHookVoid(net.dv8tion.jda.api.interactions.InteractionHook hook, MessageEmbed embed, List<Button> buttons) {
        hook.editOriginalEmbeds(embed)
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