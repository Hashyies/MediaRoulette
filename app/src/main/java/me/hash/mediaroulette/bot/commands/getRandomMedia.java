package me.hash.mediaroulette.bot.commands;

import me.hash.mediaroulette.bot.Bot;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class getRandomMedia extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random")) return;

        event.deferReply().queue();
        Bot.executor.execute(() -> {
            // boolean shouldContinue = event.getOption("shouldcontinue") != null && event.getOption("shouldcontinue").getAsBoolean();
            // String subcommand = event.getSubcommandName();
            // String option = event.getOption(subcommand) != null ? event.getOption(subcommand).getAsString() : null;

            
        });
    }
}
