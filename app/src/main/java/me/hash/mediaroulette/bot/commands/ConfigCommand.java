package me.hash.mediaroulette.bot.commands;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.utils.ImageOptions;
import me.hash.mediaroulette.utils.Hastebin;
import me.hash.mediaroulette.utils.User;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

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

                botConfigChange(event, option);
            } else if (event.getSubcommandName().equals("user")) {
                userConfigChange(event);
            } else if (event.getSubcommandName().equals("send")) {
                User user = User.get(Main.database, event.getUser().getId());
                Map<String, ImageOptions> allImageOptions = user.getAllImageOptions();
                if (allImageOptions.isEmpty()) {
                    sendErrorEmbed(event, "Image options are empty. Please set some values.");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, ImageOptions> entry : allImageOptions.entrySet()) {
                        String imageType = entry.getKey();
                        ImageOptions imageOption = entry.getValue();
                        sb.append(imageType + ": enabled=" + imageOption.isEnabled() + ", chance="
                                + imageOption.getChance() + "\n");
                    }
                    String text = event.getOption("description").getAsString();
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("User Configuration")
                            .setDescription(text)
                            .setColor(Color.GREEN);
                            // Create a paste containing the configuration string
                    String pasteId = null;
                    try {
                        pasteId = Hastebin.createPaste(sb.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    event.replyEmbeds(embed.build())
                            .addActionRow(Button.secondary("set-config:" + pasteId, "⚙️ Set Configuration"))
                            .queue();
                }

            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("set-config:")) {
            // Retrieve the paste's ID from the button's ID
            String pasteId = event.getComponentId().substring("set-config:".length());

            // Retrieve the paste's content using the pastes.io API
            String configString = null;
            try {
                configString = Hastebin.getPaste(pasteId);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Parse the configuration string and set the user's configuration
            String[] configEntries = configString.split("\n");
            for (String configEntry : configEntries) {
                String[] parts = configEntry.split(": ");
                String imageType = parts[0];
                String[] values = parts[1].split(", ");
                boolean enabled = Boolean.parseBoolean(values[0].split("=")[1]);
                double chance = Double.parseDouble(values[1].split("=")[1]);
                User user = User.get(Main.database, event.getUser().getId());
                user.setChances(new ImageOptions(imageType, enabled, chance));
            }
            event.reply("Your configuration has been updated!").queue();
        }
    }

    public void userConfigChange(SlashCommandInteractionEvent event) {
        if (event.getName().equals("config") && event.getSubcommandName().equals("user")) {
            String option = event.getOption("option").getAsString();
            String value = event.getOption("value").getAsString();
            User user = User.get(Main.database, event.getUser().getId());

            switch (option) {
                case "nsfw":
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        sendErrorEmbed(event, "Invalid value for NSFW: " + value);
                        return;
                    }
                    boolean nsfwEnabled = Boolean.parseBoolean(value);
                    user.setNsfwEnabled(nsfwEnabled);
                    sendSuccessEmbed(event, "Set NSFW to: " + value);
                    break;
                case "chances":
                    String[] chances = value.split(",");
                    ImageOptions[] options = new ImageOptions[chances.length];
                    for (int i = 0; i < chances.length; i++) {
                        String[] chanceData = chances[i].split(":");
                        String imageType = chanceData[0];
                        if (!ImageOptions.getDefaultOptions().stream()
                                .map(ImageOptions::getImageType)
                                .anyMatch(type -> type.equals(imageType))) {
                            sendErrorEmbed(event, "Invalid image option: " + imageType);
                            return;
                        }
                        double chance = Double.parseDouble(chanceData[1]);
                        options[i] = new ImageOptions(imageType, true, chance);
                    }
                    user.setChances(options);
                    sendSuccessEmbed(event, "Set chances");
                    break;
                case "enable":
                case "disable":
                    boolean enable = option.equals("enable");
                    if (!ImageOptions.getDefaultOptions().stream()
                            .map(ImageOptions::getImageType)
                            .anyMatch(imageType -> imageType.equals(value))) {
                        sendErrorEmbed(event, "Invalid image option: " + value);
                        return;
                    }
                    ImageOptions imageOption = user.getImageOptions(value);
                    if (imageOption == null) {
                        double defaultChance = ImageOptions.getDefaultOptions().stream()
                                .filter(opt -> opt.getImageType().equals(value))
                                .findFirst()
                                .map(ImageOptions::getChance)
                                .orElse(0.0);
                        imageOption = new ImageOptions(value, enable, defaultChance);
                    } else {
                        imageOption.setEnabled(enable);
                    }
                    user.setChances(imageOption);
                    sendSuccessEmbed(event, (enable ? "Enabled" : "Disabled") + " image option: " + value);
                    break;
                default:
                    sendErrorEmbed(event, "Invalid option: " + option);
                    break;
            }
        }
    }

    private void sendSuccessEmbed(SlashCommandInteractionEvent event, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Success")
                .setDescription(description)
                .setColor(Color.GREEN);
        event.replyEmbeds(embed.build()).queue();
    }

    private void sendErrorEmbed(SlashCommandInteractionEvent event, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Error")
                .setDescription(description)
                .setColor(Color.RED);
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void botConfigChange(SlashCommandInteractionEvent event, String option) {
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