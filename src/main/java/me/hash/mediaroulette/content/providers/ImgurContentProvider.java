package me.hash.mediaroulette.content.providers;

import me.hash.mediaroulette.content.ContentInfo;
import me.hash.mediaroulette.content.ContentProvider;
import me.hash.mediaroulette.utils.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

public class ImgurContentProvider implements ContentProvider {
    private static final String[] IMAGE_FORMATS = {"jpg", "png", "gif"};
    private static final int MAX_ATTEMPTS = 120;
    private static final Random RANDOM = new Random();

    @Override
    public ContentInfo getRandomContent() {
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS) {
            String imgurId = ImageUtils.getRandomImgurId();
            String imageUrl = "https://i.imgur.com/" + imgurId + "." +
                    IMAGE_FORMATS[RANDOM.nextInt(IMAGE_FORMATS.length)];
            try {
                URL url = new URL(imageUrl);
                BufferedImage image = ImageIO.read(url);
                if (image == null ||
                        (image.getWidth() == 198 && image.getHeight() == 160) ||
                        (image.getWidth() == 161 && image.getHeight() == 81) ||
                        image.getWidth() < 64 || image.getHeight() < 64) {
                    attempt++;
                    continue;
                }
                String title = "Here is your random Imgur picture!";
                String description = String.format("🌐 Source: Imgur\n🔁 Failed Image Count: %d", attempt);
                return new ContentInfo(title, description, imageUrl);
            } catch (IOException e) {
                attempt++;
            }
        }
        return null;
    }
}
