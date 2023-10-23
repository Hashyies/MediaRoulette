package me.hash.mediaroulette.bot.commands;

import net.dv8tion.jda.api.interactions.Interaction;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.random.RandomImage;
import me.hash.mediaroulette.utils.random.RandomMedia;
import me.hash.mediaroulette.utils.random.RandomReddit;
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
    ALL("ALL");

    private final String name;

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
                    String subreddit = option != null ? option : "funny";
                    if (!RandomReddit.doesSubredditExist(subreddit)) {
                        Embeds.sendErrorEmbed(event, "Hey...", "Sadly this subreddit doesn't exist...");
                        return Map.of("image", "end");
                    }
                    Map<String, String> s = RandomReddit.getRandomReddit(subreddit);
                    if (s.get("image") == null) {
                        Embeds.sendErrorEmbed(event, "This subreddit is invalid or an error occurred, please contact the owner!", "This subreddit has issues: " + s.get("subreddit"));
                        return Map.of("image", "end");
                    }
                    return s;

                case TENOR:
                    return RandomImage.getTenor(option);

                case IMGUR:
                    return RandomImage.getImgurImage();

                case _4CHAN:
                String board = option != null ? option : null;
                    if (!RandomImage.BOARDS.contains(board)) {
                        Embeds.sendErrorEmbed(event, "Hey...", "This board doesn't exist...");
                        return Map.of("image", "end");
                    }
                    return RandomImage.get4ChanImage(board);

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

                case ALL:
                    me.hash.mediaroulette.utils.User user = me.hash.mediaroulette.utils.User.get(Main.database,
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
