package me.hash.mediaroulette.bot.commands;

import me.hash.mediaroulette.Main;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ConfigCommand extends ListenerAdapter {
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("config") && event.getSubcommandName().equals("bot")) {
            // Add choices based on the input
            event.replyChoiceStrings(Main.CHOICES_BOT).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("config") && event.getSubcommandName().equals("bot")) {
            
        }
    }
}
