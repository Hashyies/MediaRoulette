package me.hash.mediaroulette.utils.media.image_generation;

import me.hash.mediaroulette.utils.media.image_generation.components.TextComponent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ImageRenderer {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    private final Theme theme;
    private final int width;
    private final int height;
    private final double scaleFactor;

    public ImageRenderer(Theme theme, int baseWidth, int baseHeight, double scaleFactor) {
        this.theme = theme;
        this.width = (int) (baseWidth * scaleFactor);
        this.height = (int) (baseHeight * scaleFactor);
        this.scaleFactor = scaleFactor;
    }

    public BufferedImage render(List<TextComponent> components) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            setupRenderingHints(g2d);
            renderBackground(g2d);
            renderOverlay(g2d);
            renderTextBox(g2d, components);
        } finally {
            g2d.dispose();
        }

        return image;
    }

    private void setupRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    private void renderBackground(Graphics2D g2d) {
        String bgPath = theme.getBackgroundImage();
        System.out.println("Background image path: " + bgPath);
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(bgPath)) {
            if (inputStream != null) {
                System.out.println("Background image found.");
                BufferedImage background = ImageIO.read(inputStream);
                System.out.println("Background image loaded: " + background.getWidth() + "x" + background.getHeight());
                // Scale background to fit while maintaining aspect ratio
                Image scaledBackground = background.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                g2d.drawImage(scaledBackground, 0, 0, null);
                System.out.println("Background image rendered at (0,0) with size " + width + "x" + height);
            } else {
                System.out.println("Background image not found at '" + bgPath + "', falling back to gradient.");
                renderGradientBackground(g2d);
            }
        } catch (IOException e) {
            System.out.println("Error loading background image '" + bgPath + "': " + e.getMessage());
            e.printStackTrace();
            renderGradientBackground(g2d);
        }
    }

    private void renderGradientBackground(Graphics2D g2d) {
        GradientPaint gradient = new GradientPaint(
                0, 0, theme.getColorPalette().getPrimaryColor(),
                width, height, theme.getColorPalette().getSecondaryColor()
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);
    }

    private void renderOverlay(Graphics2D g2d) {
        Color overlayColor = theme.getColorPalette().getOverlayColor();

        // Only render if overlay has some transparency (not fully opaque)
        if (overlayColor.getAlpha() > 0 && overlayColor.getAlpha() < 255) {
            // Set composite mode for proper alpha blending
            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    overlayColor.getAlpha() / 255.0f));

            // Create color without alpha channel (since we're using composite)
            Color solidColor = new Color(overlayColor.getRed(),
                    overlayColor.getGreen(),
                    overlayColor.getBlue());
            g2d.setColor(solidColor);
            g2d.fillRect(0, 0, width, height);

            // Restore original composite
            g2d.setComposite(originalComposite);
        }
    }

    private void renderTextBox(Graphics2D g2d, List<TextComponent> components) {
        Font font = loadFont();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        // Calculate box dimensions
        BoxDimensions boxDims = calculateBoxDimensions(g2d, components, fm);

        // Render shadow
        renderShadow(g2d, boxDims);

        // Render main box
        renderBox(g2d, boxDims);

        // Render border
        renderBorder(g2d, boxDims);

        // Render content
        renderContent(g2d, components, boxDims, fm);
    }

    private Font loadFont() {
        Theme.TextStyle textStyle = theme.getTextStyle();
        int fontSize = (int) (textStyle.getFontSize() * scaleFactor);

        try (InputStream fontStream = getClass().getClassLoader()
                .getResourceAsStream("fonts/" + textStyle.getFontFamily() + ".ttf")) {
            if (fontStream != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                int style = "BOLD".equals(textStyle.getFontWeight()) ? Font.BOLD : Font.PLAIN;
                return baseFont.deriveFont(style, fontSize);
            }
        } catch (Exception e) {
            // Fallback to system font
        }

        int style = "BOLD".equals(textStyle.getFontWeight()) ? Font.BOLD : Font.PLAIN;
        return new Font("SansSerif", style, fontSize);
    }

    private BoxDimensions calculateBoxDimensions(Graphics2D g2d, List<TextComponent> components, FontMetrics fm) {
        int totalWidth = 0;
        int maxHeight = fm.getHeight();

        for (TextComponent component : components) {
            if (component.isEmoji()) {
                totalWidth += maxHeight + 5; // Emoji size + spacing
            } else {
                totalWidth += fm.stringWidth(component.getContent());
            }
        }

        Theme.BoxStyle boxStyle = theme.getBoxStyle();
        int paddingX = (int) (boxStyle.getPaddingX() * scaleFactor);
        int paddingY = (int) (boxStyle.getPaddingY() * scaleFactor);

        int boxWidth = totalWidth + paddingX * 2;
        int boxHeight = maxHeight + paddingY * 2;
        int boxX = (width - boxWidth) / 2;
        int boxY = (height - boxHeight) / 2;

        return new BoxDimensions(boxX, boxY, boxWidth, boxHeight, paddingX, paddingY, maxHeight);
    }

    private void renderShadow(Graphics2D g2d, BoxDimensions boxDims) {
        Theme.BoxStyle boxStyle = theme.getBoxStyle();
        int shadowOffsetX = (int) (boxStyle.getShadowOffsetX() * scaleFactor);
        int shadowOffsetY = (int) (boxStyle.getShadowOffsetY() * scaleFactor);
        int cornerRadius = (int) (boxStyle.getCornerRadius() * scaleFactor);

        g2d.setColor(theme.getColorPalette().getShadowColor());
        g2d.fillRoundRect(
                boxDims.x + shadowOffsetX,
                boxDims.y + shadowOffsetY,
                boxDims.width,
                boxDims.height,
                cornerRadius,
                cornerRadius
        );
    }

    private void renderBox(Graphics2D g2d, BoxDimensions boxDims) {
        int cornerRadius = (int) (theme.getBoxStyle().getCornerRadius() * scaleFactor);
        g2d.setColor(theme.getColorPalette().getPrimaryColor());
        g2d.fillRoundRect(boxDims.x, boxDims.y, boxDims.width, boxDims.height, cornerRadius, cornerRadius);
    }

    private void renderBorder(Graphics2D g2d, BoxDimensions boxDims) {
        Theme.BoxStyle boxStyle = theme.getBoxStyle();
        int borderWidth = (int) (boxStyle.getBorderWidth() * scaleFactor);
        int cornerRadius = (int) (boxStyle.getCornerRadius() * scaleFactor);

        g2d.setColor(theme.getColorPalette().getSecondaryColor());
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.drawRoundRect(boxDims.x, boxDims.y, boxDims.width, boxDims.height, cornerRadius, cornerRadius);
    }

    private void renderContent(Graphics2D g2d, List<TextComponent> components,
                               BoxDimensions boxDims, FontMetrics fm) {
        int currentX = boxDims.x + boxDims.paddingX;
        int currentY = boxDims.y + boxDims.paddingY + fm.getAscent();

        for (TextComponent component : components) {
            if (component.isEmoji()) {
                currentX += renderEmoji(g2d, component, currentX, currentY, boxDims.textHeight, fm);
            } else {
                currentX += renderText(g2d, component, currentX, currentY, fm);
            }
        }
    }

    private int renderEmoji(Graphics2D g2d, TextComponent component, int x, int y,
                            int emojiSize, FontMetrics fm) {
        try {
            BufferedImage emoji = loadEmojiImage(component.getEmojiUrl());
            int adjustedY = y - emojiSize + fm.getDescent();
            g2d.drawImage(emoji, x, adjustedY, emojiSize, emojiSize, null);
            return emojiSize + 5; // Return width consumed + spacing
        } catch (IOException e) {
            // Skip emoji if loading fails
            return 0;
        }
    }

    private int renderText(Graphics2D g2d, TextComponent component, int x, int y, FontMetrics fm) {
        g2d.setColor(theme.getColorPalette().getTextColor());
        g2d.drawString(component.getContent(), x, y);
        return fm.stringWidth(component.getContent());
    }

    private BufferedImage loadEmojiImage(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to load emoji: " + response);
            }
            return ImageIO.read(response.body().byteStream());
        }
    }

    private static class BoxDimensions {
        final int x, y, width, height, paddingX, paddingY, textHeight;

        BoxDimensions(int x, int y, int width, int height, int paddingX, int paddingY, int textHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.paddingX = paddingX;
            this.paddingY = paddingY;
            this.textHeight = textHeight;
        }
    }
}