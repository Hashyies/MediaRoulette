package me.hash.mediaroulette.bot.commands;

import java.awt.Color;
import java.io.IOException;

import me.hash.mediaroulette.utils.RandomImage;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class randomQuery extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random-google"))
            return;
        event.deferReply().queue();

        Bot.executor.execute(() -> {
            if (!Bot.config.get("GOOGLE", Boolean.class)) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Error");
                embedBuilder.setDescription("This command is disabled by the bot owner");
                embedBuilder.setColor(Color.RED);
                event.getHook().sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                return;
            }
            // Get the search query from the command options
            String query = event.getOption("query").getAsString();

            // Get a random image from Google using the provided search query
            String image_url;
            try {
                image_url = RandomImage.getGoogleQueryImage(query);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            Embeds.sendImageEmbed(event, "Here is your random Google Image:", image_url, false);
        });
    }

}
