package me.hash.mediaroulette.bot.commands;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
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
            String option = event.getOption("option").getAsString();
            if (!Main.CHOICES_BOT.contains(option)) {
                event.reply("This choice is not valid!").setEphemeral(true).queue();
                return;
            }

            validateConfigChange(event, option);
        }
    }

    private void validateConfigChange(SlashCommandInteractionEvent event, String option) {
        String value = event.getOption("value").getAsString();
        boolean enabled;

        if (value.equalsIgnoreCase("true")) {
            enabled = true;
        } else if (value.equalsIgnoreCase("false")) {
            enabled = false;
        } else {
            event.reply("Incorrect input: only use false or true").queue();
            return;
        }

        if (option.equals("NSFW_WEBHOOK")) {
            if (!Main.containsKey(Main.env.entries(), option)) {
                event.reply("NSFW_WEBHOOK Not found in .env").setEphemeral(true).queue();
                return;
            }
        }

        if (option.equals("SFW_WEBHOOK")) {
            if (!Main.containsKey(Main.env.entries(), option)) {
                event.reply("SFW_WEBHOOK Not found in .env").setEphemeral(true).queue();
                return;
            }
        }

        if (option.equals("REDDIT")) {
            if (!Main.containsKey(Main.env.entries(), option)) {
                event.reply("REDDIT Not found in .env").setEphemeral(true).queue();
                return;
            }
        }

        if (option.equals("GOOGLE")) {
            if (!Main.containsKey(Main.env.entries(), option)) {
                event.reply("GOOGLE Not found in .env").setEphemeral(true).queue();
                return;
            }
        }

        if (option.equals("GENERATED_VOICE_CHANNEL")) {
            if (!Main.containsKey(Main.env.entries(), option)) {
                event.reply("GENERATED_VOICE_CHANNEL Not found in .env").setEphemeral(true).queue();
                return;
            }
        }

        event.reply("Set " + option + " to: " + value).queue();
        Bot.config.set(option, enabled);
    }
}
