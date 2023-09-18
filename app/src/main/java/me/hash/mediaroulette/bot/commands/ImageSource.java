package me.hash.mediaroulette.bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.Color;
import java.io.IOException;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.utils.RandomImage;
import me.hash.mediaroulette.utils.random.RandomReddit;
import me.hash.mediaroulette.utils.exceptions.*;

public enum ImageSource {
    // ADDED EVEMTS FOR REDUNDANCY. MIGHT REMOVE LATER
    REDDIT("REDDIT") {
        @Override
        public String handle(Interaction event, boolean shouldContinue, String option) {
            if (isOptionDisabled("REDDIT")) {
                sendErrorEmbed(event, "This command has been disabled", "This command is globally disabled!");
            }
            String subreddit = option != null ? option : "funny";
            try {
                if (!RandomReddit.doesSubredditExist(subreddit)) {
                    sendErrorEmbed(event, "Hey...", "Sadly this subreddit doesnt exist...");
                }
                String[] s = RandomReddit.getRandomReddit(subreddit);

                if (s[0] == null) {
                    sendErrorEmbed(event, "This subreddit is invalid or an error occured, please contact the owner!", "This subreddit has issues: " + s[1]);
                    return null;
                }

                return s[0];
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    },
    TENOR("TENOR") {
        @Override
        public String handle(Interaction event, boolean shouldContinue, String option) {
            if (isOptionDisabled("TENOR")) {
                sendErrorEmbed(event, "This command has been disabled", "This command is globally disabled!");
            }
            String query = option != null ? option : "test";
            try {
                return RandomImage.getTenor(query);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    },
    _4CHAN("_4CHAN") {
        @Override
        public String handle(Interaction event, boolean shouldContinue, String option) {
            if (isOptionDisabled("_4CHAN")) {
                sendErrorEmbed(event, "This command has been disabled", "This command is globally disabled!");
            }
            String board = option != null ? option : "r";
            return RandomImage.get4ChanImage(board)[0];
        }
    },
    GOOGLE("GOOGLE") {
        @Override
        public String handle(Interaction event, boolean shouldContinue, String option) {
            if (isOptionDisabled("GOOGLE")) {
                sendErrorEmbed(event, "This command has been disabled", "This command is globally disabled!");
            }
            String query = option != null ? option : "test";
            try {
                return RandomImage.getGoogleQueryImage(query);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    },
    PICSUM("PICSUM") {
        @Override
        public String handle(Interaction event, boolean shouldContinue, String option) {
            if (isOptionDisabled("PICSUM")) {
                sendErrorEmbed(event, "This command has been disabled", "This command is globally disabled!");
            }
            return RandomImage.getPicSumImage();
        }
    },
    RULE34XXX("RULE34XXX") {
        @Override
        public String handle(Interaction event, boolean shouldContinue, String option) {
            if (isOptionDisabled("RULE34XXX")) {
                sendErrorEmbed(event, "This command has been disabled", "This command is globally disabled!");
            }

            return RandomImage.getRandomRule34xxx();
        }

    },
    ALL("ALL") {
        @Override
        public String handle(Interaction event, boolean shouldContinue, String option) {
            me.hash.mediaroulette.utils.User user = me.hash.mediaroulette.utils.User.get(Main.database,
                    event.getMember().getId());
            try {
                return user.getImage();
            } catch (NoEnabledOptionsException | InvalidChancesException e) {
                sendErrorEmbed(event, "Error", e.getMessage());
            }
            return null;
        }
    };

    private static boolean isOptionDisabled(String option) {
        return !Bot.config.getOrDefault(option, true, Boolean.class);
    }

    private static void sendErrorEmbed(Interaction event, String title, String description) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle(title);
        errorEmbed.setDescription(description);
        errorEmbed.setColor(Color.RED);
        if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).getHook().sendMessageEmbeds(errorEmbed.build()).queue();
        } else if (event instanceof ButtonInteractionEvent) {
            ((ButtonInteractionEvent) event).getHook().sendMessageEmbeds(errorEmbed.build()).queue();
        }
    }

    private final String name;

    ImageSource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract String handle(Interaction event, boolean shouldContinue, String option);

    public static ImageSource fromName(String name) {
        for (ImageSource source : values()) {
            if (source.getName().equalsIgnoreCase(name)) {
                return source;
            }
        }
        return null;
    }
}
