package me.hash.mediaroulette.bot.commands;

import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.RandomImage;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class getRule34xxx extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random-rule34xxx"))
            return;
        event.deferReply().queue();

        Bot.executor.execute(() -> {

        // Reply with an embed containing the random image
        Embeds.sendImageEmbed(event, "Here is your random Rule34.xxx Image:", RandomImage.getRandomRule34xxx(), false);
    });
    }

}
