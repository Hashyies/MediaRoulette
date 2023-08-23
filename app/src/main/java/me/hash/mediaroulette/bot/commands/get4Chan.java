package me.hash.mediaroulette.bot.commands;

import java.awt.Color;

import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.RandomImage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class get4Chan extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random-4chan"))
            return;
        event.deferReply().queue();
        Bot.executor.execute(() -> {
            // Get the search query from the command options
            String board = null;
            if (event.getOption("board") != null) {
                board = event.getOption("board").getAsString();
            }

            if (board != null && !RandomImage.BOARDS.contains(board)) {
                // The subreddit does not exist, reply with an error embed
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Error");
                embedBuilder.setDescription("The board '" + board + "' does not exist.");
                embedBuilder.setColor(Color.RED);
                event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                return;
            }

            // Reply with an embed containing the random image
            Embeds.sendImageEmbed(event, "Here is your random 4Chan Image:", RandomImage.get4ChanImage(board)[0],
                    false);

        });
    }

}