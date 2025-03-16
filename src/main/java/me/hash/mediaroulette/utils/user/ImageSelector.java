package me.hash.mediaroulette.utils.user;

import me.hash.mediaroulette.bot.commands.images.ImageSource;
import me.hash.mediaroulette.model.ImageOptions;
import me.hash.mediaroulette.exceptions.InvalidChancesException;
import me.hash.mediaroulette.exceptions.NoEnabledOptionsException;
import me.hash.mediaroulette.content.RandomImage;
import me.hash.mediaroulette.content.RandomMedia;
import me.hash.mediaroulette.content.RandomText;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ImageSelector {
    private final Map<String, ImageOptions> userImageOptions;
    private final Random random = new Random();

    public ImageSelector(Map<String, ImageOptions> userImageOptions) {
        this.userImageOptions = userImageOptions;
    }

    public Map<String, String> selectImage() throws NoEnabledOptionsException, InvalidChancesException {
        List<ImageOptions> defaultImageOptions = ImageOptions.getDefaultOptions();
        PriorityQueue<ImageOptions> queue = new PriorityQueue<>(Comparator.comparingDouble(ImageOptions::getChance));

        double totalChance = 0;
        for (ImageOptions defaultOption : defaultImageOptions) {
            String imageType = defaultOption.getImageType();
            ImageOptions userOption = userImageOptions.get(imageType);
            if (userOption != null && userOption.isEnabled()) {
                totalChance += userOption.getChance();
                queue.add(userOption);
            } else if (defaultOption.isEnabled()) {
                totalChance += defaultOption.getChance();
                queue.add(defaultOption);
            }
        }

        if (queue.isEmpty()) {
            throw new NoEnabledOptionsException("All image options are disabled");
        }

        normalizeProbabilities(queue, totalChance);

        return getRandomImageFromQueue(queue);
    }

    private void normalizeProbabilities(PriorityQueue<ImageOptions> queue, double totalChance) {
        if (totalChance != 100) {
            double difference = 100 - totalChance;
            double additionalChance = difference / queue.size();
            for (ImageOptions option : queue) {
                option.setChance(option.getChance() + additionalChance);
            }
        }
    }

    private Map<String, String> getRandomImageFromQueue(PriorityQueue<ImageOptions> queue) throws InvalidChancesException {
        double rand = random.nextDouble() * 100;
        double cumulativeProbability = 0;
        while (!queue.isEmpty()) {
            ImageOptions selectedOption = queue.poll();
            cumulativeProbability += selectedOption.getChance();
            if (rand <= cumulativeProbability) {
                try {
                    return getImageByType(selectedOption.getImageType());
                } catch (IOException e) {
                    throw new InvalidChancesException("Failed to fetch image from source: " + selectedOption.getImageType());
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new InvalidChancesException("Failed to select an image.");
    }

    private Map<String, String> getImageByType(String imageType) throws IOException, ExecutionException, InterruptedException {
        return switch (imageType) {
            case "4chan" -> RandomImage.get4ChanImage(null);
            case "picsum" -> RandomImage.getPicSumImage();
            case "imgur" -> RandomImage.getImgurImage();
            case "reddit" -> ImageSource.randomRedditService.getRandomReddit(null);
            case "rule34xxx" -> RandomImage.getRandomRule34xxx();
            case "tenor" -> RandomImage.getTenor(null);
            case "google" -> RandomImage.getGoogleQueryImage(null);
            case "movies" -> RandomMedia.randomMovie();
            case "tvshow" -> RandomMedia.randomTVShow();
            case "youtube" -> RandomMedia.getRandomYoutube();
            case "short" -> RandomMedia.getRandomYoutubeShorts();
            case "urban" -> RandomText.getRandomUrbanWord(null);
            default -> throw new IllegalArgumentException("Unknown image type: " + imageType);
        };
    }
}