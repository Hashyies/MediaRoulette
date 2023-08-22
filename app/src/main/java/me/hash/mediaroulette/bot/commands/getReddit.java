package me.hash.mediaroulette.bot.commands;

import java.awt.Color;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.utils.random.RandomReddit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class getReddit extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random-reddit"))
            return;
        event.deferReply().queue();

        Bot.executor.execute(() -> {
        // Get the search query from the command options
        String subreddit = null;
        if (event.getOption("subreddit") != null) {
            subreddit = event.getOption("subreddit").getAsString();
        }

        // Check if the user is on cooldown
        long userId = event.getUser().getIdLong();
        if (Bot.COOLDOWNS.containsKey(userId)
                && System.currentTimeMillis() - Bot.COOLDOWNS.get(userId) < Bot.COOLDOWN_DURATION) {
            // The user is on cooldown, reply with an embed
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Slow down dude...");
            embedBuilder.setDescription(
                    "Please wait for " + Bot.COOLDOWN_DURATION / 1000 + " seconds before using this command again.");
            embedBuilder.setColor(Color.RED);
            event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
            return;
        }

        // Update the user's cooldown
        Bot.COOLDOWNS.put(userId, System.currentTimeMillis());

        // Check if the subreddit exists
        try {
            if (subreddit != null && !RandomReddit.doesSubredditExist(subreddit)) {
                // The subreddit does not exist, reply with an error embed
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Error");
                embedBuilder.setDescription("The subreddit '" + subreddit + "' does not exist.");
                embedBuilder.setColor(Color.RED);
                event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get a random image from Reddit using the provided search query
        String image_url;
        try {
            image_url = RandomReddit.getRandomReddit(subreddit);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        // Reply with an embed containing the random image
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Here is your random Reddit Image:");
        embedBuilder.setUrl(image_url);
        embedBuilder.setImage(image_url);
        embedBuilder.setColor(Color.CYAN);
        embedBuilder.setFooter("Current time: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        User user = event.getUser();
        embedBuilder.setAuthor(user.getName(), null, user.getEffectiveAvatarUrl());

        Button safe = Button.success("safe", "Safe")
                .withEmoji(Emoji.fromUnicode("✔️"));
        Button nsfw = Button.danger("nsfw", "NSFW")
                .withEmoji(Emoji.fromUnicode("🔞"));

        event.getHook().sendMessageEmbeds(embedBuilder.build())
                .addActionRow(safe, nsfw)
                .queue();

            Bot.config.set("image_generated", new BigInteger(Bot.config.getOrDefault("image_generated", "0", String.class))
                .add(new BigInteger(String.valueOf(1))).toString());
    });
    }

}
