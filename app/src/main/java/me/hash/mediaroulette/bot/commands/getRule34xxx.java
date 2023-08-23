package me.hash.mediaroulette.bot.commands;

import java.awt.Color;

import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.RandomImage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class getRule34xxx extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random-rule34xxx"))
            return;
        event.deferReply().queue();

        Bot.executor.execute(() -> {
            if (!Bot.config.get("RULE34XXX", Boolean.class)) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Error");
                embedBuilder.setDescription("This command is disabled by the bot owner");
                embedBuilder.setColor(Color.RED);
                event.getHook().sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                return;
            }

            // Reply with an embed containing the random image
            Embeds.sendImageEmbed(event, "Here is your random Rule34.xxx Image:", RandomImage.getRandomRule34xxx(),
                    false);
        });
    }

}
