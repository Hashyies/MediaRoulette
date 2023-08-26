package me.hash.mediaroulette.bot.commands;

import java.awt.Color;
import java.io.IOException;

import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.random.RandomReddit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class getReddit extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random-reddit"))
            return;
        event.deferReply().queue();

        Bot.executor.execute(() -> {
            if (!Bot.config.getOrDefault("REDDIT", true, Boolean.class)) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Error");
                embedBuilder.setDescription("This command is disabled by the bot owner");
                embedBuilder.setColor(Color.RED);
                event.getHook().sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                return;
            }
            // Get the search query from the command options
            String subreddit = null;
            if (event.getOption("subreddit") != null) {
                subreddit = event.getOption("subreddit").getAsString();
            }

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

            try {
                Embeds.sendImageEmbed(event, "Here is your random Reddit iamge:",
                        RandomReddit.getRandomReddit(subreddit), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
