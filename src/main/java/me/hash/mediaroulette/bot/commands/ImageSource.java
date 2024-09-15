package me.hash.mediaroulette.bot.commands;

import me.hash.mediaroulette.utils.random.reddit.RandomRedditService;
import me.hash.mediaroulette.utils.random.reddit.RedditClient;
import me.hash.mediaroulette.utils.random.reddit.SubredditManager;
import me.hash.mediaroulette.utils.user.User;
import net.dv8tion.jda.api.interactions.Interaction;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.random.RandomImage;
import me.hash.mediaroulette.utils.random.RandomMedia;
import me.hash.mediaroulette.utils.random.RandomText;
import me.hash.mediaroulette.utils.exceptions.*;

public enum ImageSource {
    REDDIT("REDDIT"),
    TENOR("TENOR"),
    _4CHAN("4CHAN"),
    GOOGLE("GOOGLE"),
    IMGUR("IMGUR"),
    PICSUM("PICSUM"),
    RULE34XXX("RULE34XXX"),
    MOVIE("MOVIE"),
    TVSHOW("TVSHOW"),
    URBAN("URBAN"),
    YOUTUBE("YOUTUBE"),
    SHORT("SHORT"),
    ALL("ALL");

    private final String name;

    public static final RedditClient redditClient = new RedditClient();
    public static final SubredditManager subredditManager = new SubredditManager(redditClient);
    public static final RandomRedditService randomRedditService = new RandomRedditService(redditClient, subredditManager);

    ImageSource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }


    public Map<String, String> handle(Interaction event, boolean shouldContinue, String option) {
        if (isOptionDisabled(this.name)) {
            Embeds.sendErrorEmbed(event, "This command has been disabled", "This command is globally disabled!");
            return null;
        }

        try {
            switch (this) {
                case REDDIT:
                    try {
                        String subreddit = (option != null)
                                ? option
                                : subredditManager.getRandomSubreddit();

                        // Check if the subreddit exists; if not, send an error message and return
                        if (!subredditManager.doesSubredditExist(subreddit)) {
                            Embeds.sendErrorEmbed(event, "Hey...", "Sadly, this subreddit doesn't exist...");
                            return Map.of("image", "end");
                        }

                        Map<String, String> redditPost = randomRedditService.getRandomReddit(subreddit);

                        // Check if the image is null, indicating an issue with the subreddit or the post
                        if (redditPost == null) {
                            Embeds.sendErrorEmbed(event,
                                    "This subreddit is invalid or an error occurred, please contact the owner!",
                                    "This subreddit has issues: " + subreddit);
                            return Map.of("image", "end");
                        }

                        return redditPost;

                    } catch (IOException e) {
                        e.printStackTrace();
                        Embeds.sendErrorEmbed(event, "Error", "An unexpected error occurred while fetching data from Reddit. Please try again later.");
                        return Map.of("image", "end");
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                case TENOR:
                    return RandomImage.getTenor(option);

                case IMGUR:
                    return RandomImage.getImgurImage();

                case _4CHAN:
                    if (!RandomImage.BOARDS.contains(option) && !(option == null)) {
                        Embeds.sendErrorEmbed(event, "Hey...", "This board doesn't exist...");
                        return Map.of("image", "end");
                    }
                    return RandomImage.get4ChanImage(option);

                case GOOGLE:
                    return RandomImage.getGoogleQueryImage(option);

                case PICSUM:
                    return RandomImage.getPicSumImage();

                case RULE34XXX:
                    return RandomImage.getRandomRule34xxx();
                case MOVIE:
                    return RandomMedia.randomMovie();
                case TVSHOW:
                    return RandomMedia.randomTVShow();
                case URBAN:
                    Map<String, String> map = RandomText.getRandomUrbanWord(option);
                    if (map.containsKey("error")) {
                        Embeds.sendErrorEmbed(event, "Error", map.get("error"));
                        return Map.of("image", "end");
                    } else return map;
                case YOUTUBE:
                    return RandomMedia.getRandomYoutube();
                case SHORT:
                    return RandomMedia.getRandomYoutubeShorts();
                case ALL:
                    User user = User.get(Main.database,
                            event.getUser().getId());
                    return user.getImage();

                default:
                    return null;
            }
        } catch (IOException | NoEnabledOptionsException | InvalidChancesException e) {
            Embeds.sendErrorEmbed(event, "Error", e.getMessage());
            return Map.of("image", "end");
        }
    }

    private static boolean isOptionDisabled(String option) {
        return !Bot.config.getOrDefault(option, true, Boolean.class);
    }

    public static Optional<ImageSource> fromName(String name) {
        for (ImageSource source : values()) {
            if (source.getName().equalsIgnoreCase(name)) {
                return Optional.of(source);
            }
        }
        return Optional.empty();
    }
}
