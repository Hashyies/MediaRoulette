package me.hash.mediaroulette.bot.commands;

import java.awt.Color;
import java.io.IOException;

import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.RandomImage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class getTenor extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random-tenor"))
            return;

        event.deferReply().queue();
        Bot.executor.execute(() -> {
            if (!Bot.config.getOrDefault("TENOR", true, Boolean.class)) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Error");
                embedBuilder.setDescription("This command is disabled by the bot owner");
                embedBuilder.setColor(Color.RED);
                event.getHook().sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                return;
            }
            // Reply with random PicSum image
            try {
                Embeds.sendImageEmbed(event, "Here is your random Tenor Image:", RandomImage.getTenor(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

