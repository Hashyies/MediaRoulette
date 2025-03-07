package me.hash.mediaroulette.utils.media;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import okhttp3.OkHttpClient;

import okhttp3.Request;
import okhttp3.Response;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageGenerator {

    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Generates an image with the given text.
     *
     * @param text The text to include in the image.
     * @return A byte array representing the generated image in PNG format.
     */
    public static byte[] generateImage(String text) {
        // Increase resolution for higher-quality output
        int width = (int) (500 * 2.5);
        int height = (int) (300 * 2.5);

        // Create an empty image
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        // Enable anti-aliasing and high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Load the background image from resources
        try (InputStream inputStream = ImageGenerator.class.getClassLoader().getResourceAsStream("images/Background.jpg")) {
            if (inputStream == null) {
                throw new IOException("Background image not found in resources!");
            }
            BufferedImage background = ImageIO.read(inputStream);
            g2d.drawImage(background, 0, 0, width, height, null); // Scale background to fit the canvas
        } catch (IOException e) {
            e.printStackTrace();
            g2d.setColor(Color.BLACK); // Use black background as a fallback
            g2d.fillRect(0, 0, width, height);
        }

        // Add a light translucent black overlay to darken the background
        g2d.setColor(new Color(0, 0, 0, 150)); // Black with 150 alpha
        g2d.fillRect(0, 0, width, height);

        // Load Montserrat font from resources
        Font customFont = loadFont((int) (36 * 2.5));
        g2d.setFont(customFont);

        // Parse text into components (plain text and emojis)
        List<RenderedTextComponent> components = parseTextWithTwemoji(text);

        // Measure total width and height of the components (text and emojis)
        FontMetrics fm = g2d.getFontMetrics();
        int totalWidth = 0; // Total width of the text and emojis
        int maxHeight = fm.getHeight(); // Baseline height

        // Calculate total width for the bounding box
        for (RenderedTextComponent component : components) {
            if (component.isImage()) {
                totalWidth += maxHeight; // Emojis are square, same height as text
            } else {
                totalWidth += fm.stringWidth(component.getText()); // Width of plain text
            }
        }

        // Define padding and calculate rectangle dimensions
        int paddingX = 50; // Padding for sides
        int paddingY = 30; // Padding for top and bottom
        int rectWidth = totalWidth + paddingX * 2; // Total rectangle width
        int rectHeight = maxHeight + paddingY * 2; // Total rectangle height
        int rectX = (width - rectWidth) / 2; // Centered horizontally
        int rectY = (height - rectHeight) / 2; // Centered vertically

        // Draw the sleek translucent blue box
        g2d.setColor(new Color(50, 125, 255, 150)); // Blue color with transparency
        g2d.fillRoundRect(rectX, rectY, rectWidth, rectHeight, 40, 40); // Rounded rectangle

        // Add a subtle border to the box for highlight
        g2d.setColor(new Color(255, 255, 255, 100)); // Soft white highlight
        g2d.setStroke(new BasicStroke(4)); // Thicker stroke for the border
        g2d.drawRoundRect(rectX, rectY, rectWidth, rectHeight, 40, 40); // Border matches rounded corners

        // Add a drop shadow offset behind the rectangle for depth
        g2d.setColor(new Color(0, 0, 0, 85)); // Semi-transparent black for shadow
        g2d.fillRoundRect(rectX + 5, rectY + 5, rectWidth, rectHeight, 40, 40);

        // Draw the text and/or emojis inside the rectangle
        int currentX = rectX + paddingX; // Start inside the padding of the box
        int currentY = rectY + paddingY + fm.getAscent(); // Start based on padding and text ascent
        for (RenderedTextComponent component : components) {
            if (component.isImage()) {
                // Render emoji (as an image)
                try {
                    BufferedImage emoji = loadEmojiImage(component.getEmojiUrl());
                    int emojiSize = maxHeight; // Emoji height consistent with text
                    g2d.drawImage(emoji, currentX, currentY - emojiSize + fm.getDescent(), emojiSize, emojiSize, null); // Align emoji
                    currentX += emojiSize + 5; // Add slight spacing after emoji
                } catch (IOException e) {
                    e.printStackTrace(); // Skip emoji if loading fails
                }
            } else {
                // Render plain text
                g2d.setColor(Color.WHITE); // White text for contrast
                g2d.drawString(component.getText(), currentX, currentY);
                currentX += fm.stringWidth(component.getText()) + fm.charWidth(' '); // Add width + space
            }
        }

        // Dispose the graphics context
        g2d.dispose();

        // Convert the final image to a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    /**
     * Loads the Montserrat font at the specified size.
     */
    private static Font loadFont(int size) {
        Font customFont;
        try (InputStream fontStream = ImageGenerator.class.getClassLoader().getResourceAsStream("fonts/Montserrat.ttf")) {
            if (fontStream == null) {
                throw new IOException("Montserrat font not found in resources!");
            }
            customFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(Font.BOLD, size); // Load font with set size
        } catch (Exception e) {
            e.printStackTrace();
            customFont = new Font("SansSerif", Font.BOLD, size); // Fallback font
        }
        return customFont;
    }

    /**
     * Parses a given text into a list of components, separating text and emojis.
     */
    private static List<RenderedTextComponent> parseTextWithTwemoji(String text) {
        List<RenderedTextComponent> components = new ArrayList<>();
        String regex = "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u200D]+|[^\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u200D]+"; // Emojis and text separation        Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String match = matcher.group();
            if (isEmoji(match)) {
                components.add(new RenderedTextComponent(getTwemojiUrl(match), true)); // Emoji component
            } else {
                components.add(new RenderedTextComponent(match, false)); // Text component
            }
        }
        return components;
    }

    /**
     * Determines whether the given text is an emoji.
     */
    private static boolean isEmoji(String text) {
        int codePoint = text.codePointAt(0);
        return Character.UnicodeBlock.of(codePoint).equals(Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS)
                || Character.UnicodeBlock.of(codePoint).equals(Character.UnicodeBlock.EMOTICONS)
                || Character.UnicodeBlock.of(codePoint).equals(Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS)
                || Character.UnicodeBlock.of(codePoint).equals(Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS);
    }

    /**
     * Returns the Twemoji URL for the given emoji.
     */
    private static String getTwemojiUrl(String emoji) {
        // Convert the emoji into Unicode code points in hex format
        String unicode = emoji.codePoints()
                .mapToObj(Integer::toHexString)
                .collect(Collectors.joining("-"));
        // Use the Twemoji CDN at jsdelivr
        return "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/" + unicode + ".png";
    }

    /**
     * Loads an emoji image from the given URL using OkHttpClient.
     */
    private static BufferedImage loadEmojiImage(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to load emoji image: " + response);
            }
            InputStream inputStream = response.body().byteStream();
            return ImageIO.read(inputStream);
        }
    }

    /**
     * Class representing a component of rendered text, which can be either plain text or an emoji image.
     */
    private static class RenderedTextComponent {
        private final String text;
        private final boolean isImage;

        public RenderedTextComponent(String text, boolean isImage) {
            this.text = text;
            this.isImage = isImage;
        }

        public String getText() {
            return text;
        }

        public boolean isImage() {
            return isImage;
        }

        public String getEmojiUrl() {
            return text;
        }
    }
}