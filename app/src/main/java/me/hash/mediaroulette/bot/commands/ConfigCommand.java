package me.hash.mediaroulette.bot.commands;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

public class ConfigCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("config")) {
            if (event.getSubcommandName().equals("bot")) {
                String option = event.getOption("option").getAsString();
                if (!Main.CHOICES_BOT.contains(option)) {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("Error")
                            .setDescription("This choice is not valid!")
                            .setColor(Color.RED);
                    event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                    return;
                }
    
                validateConfigChange(event, option);
            } else if (event.getSubcommandName().equals("user")) {
                // Handle the user subcommand here
                // ...
            }
        }
    }
    

    private void validateConfigChange(SlashCommandInteractionEvent event, String option) {
        String value = event.getOption("value").getAsString();
        boolean enabled = Boolean.parseBoolean(value);
    
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Error")
                    .setDescription("Incorrect input: only use false or true")
                    .setColor(Color.RED);
            event.replyEmbeds(embed.build()).queue();
            return;
        }
    
        switch (option) {
            case "NSFW_WEBHOOK":
            case "SFW_WEBHOOK":
            case "REDDIT":
            case "GOOGLE":
            case "GENERATED_VOICE_CHANNEL":
                if (!Main.containsKey(Main.env.entries(), option)) {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("Error")
                            .setDescription(option + " Not found in .env")
                            .setColor(Color.RED);
                    event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                    return;
                }
                break;
            default:
                break;
        }
    
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Success")
                .setDescription("Set " + option + " to: " + value)
                .setColor(Color.GREEN);
        event.replyEmbeds(embed.build()).queue();
        Bot.config.set(option, enabled);
    }
}