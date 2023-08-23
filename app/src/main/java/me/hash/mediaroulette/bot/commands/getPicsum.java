package me.hash.mediaroulette.bot.commands;

import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.RandomImage;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class getPicsum extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random-picsum"))
            return;

        event.deferReply().queue();
        Bot.executor.execute(() -> {
            // Reply with random PicSum image
            Embeds.sendImageEmbed(event, "Here is your random Picsum Image:", RandomImage.getPicSumImage(), false);
        });
    }
}
