package me.hash.mediaroulette.bot.commands.images;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.errorHandler;
import me.hash.mediaroulette.content.RandomImage;
import me.hash.mediaroulette.content.RandomMedia;
import me.hash.mediaroulette.content.RandomText;
import me.hash.mediaroulette.content.reddit.RandomRedditService;
import me.hash.mediaroulette.content.reddit.RedditClient;
import me.hash.mediaroulette.content.reddit.SubredditManager;
import me.hash.mediaroulette.utils.user.User;
import net.dv8tion.jda.api.interactions.Interaction;

import java.util.Map;
import java.util.Optional;

public enum ImageSource {
    REDDIT("REDDIT"),
    TENOR("TENOR"),
    _4CHAN("4CHAN"),
    GOOGLE("GOOGLE"),
    IMGUR("IMGUR"),
    PICSUM("PICSUM"),
    RULE34XXX("RULEE34XXX"),
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

    public Map<String, String> handle(Interaction event, String option) throws Exception {
        if (isOptionDisabled(this.name)) {
            errorHandler.sendErrorEmbed(event, "Command Disabled", "This command is globally disabled!");
            throw new Exception("Command Disabled");
        }

        return switch (this) {
            case REDDIT -> handleReddit(event, option);
            case TENOR -> RandomImage.getTenor(option);
            case IMGUR -> RandomImage.getImgurImage();
            case _4CHAN -> handle4Chan(event, option);
            case GOOGLE -> RandomImage.getGoogleQueryImage(option);
            case PICSUM -> RandomImage.getPicSumImage();
            case RULE34XXX -> RandomImage.getRandomRule34xxx();
            case MOVIE -> RandomMedia.randomMovie();
            case TVSHOW -> RandomMedia.randomTVShow();
            case URBAN -> handleUrban(event, option);
            case YOUTUBE -> RandomMedia.getRandomYoutube();
            case SHORT -> RandomMedia.getRandomYoutubeShorts();
            case ALL -> {
                User user = User.get(Main.database, event.getUser().getId());
                yield user.getImage();
            }
        };
    }

    private Map<String, String> handleReddit(Interaction event, String option) throws Exception {
        String subreddit = (option != null)
                ? option
                : subredditManager.getRandomSubreddit();

        // Check if the subreddit exists
        if (!subredditManager.doesSubredditExist(subreddit)) {
            errorHandler.sendErrorEmbed(event, "Invalid Subreddit", "This subreddit doesn't exist...");
            throw new Exception("Subreddit doesn't exist");
        }

        Map<String, String> redditPost = randomRedditService.getRandomReddit(subreddit);

        if (redditPost == null) {
            errorHandler.sendErrorEmbed(event, "Error", "An error occurred fetching data from Reddit.");
            throw new Exception("Error fetching Reddit data");
        }

        return redditPost;
    }

    private Map<String, String> handle4Chan(Interaction event, String option) throws Exception {
        if (option != null && !RandomImage.BOARDS.contains(option)) {
            errorHandler.sendErrorEmbed(event, "Invalid Board", "This board doesn't exist...");
            throw new Exception("Board doesn't exist");
        }
        return RandomImage.get4ChanImage(option);
    }

    private Map<String, String> handleUrban(Interaction event, String option) throws Exception {
        Map<String, String> map = RandomText.getRandomUrbanWord(option);
        if (map.containsKey("error")) {
            errorHandler.sendErrorEmbed(event, "Error", map.get("error"));
            throw new Exception(map.get("error"));
        }

        return map;
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
