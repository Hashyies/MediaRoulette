package me.hash.mediaroulette.bot.commands.config;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.errorHandler;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.utils.Hastebin;
import me.hash.mediaroulette.model.ImageOptions;
import me.hash.mediaroulette.utils.user.User;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

// TODO: Will rework this system later
public class ConfigCommand extends ListenerAdapter implements CommandHandler {

    @Override
    public CommandData getCommandData() {
        return Commands.slash("config", "Change personal, guild or bot settings")
                .addSubcommands(
                        new SubcommandData("bot", "Change settings for the bot (Admin required)")
                                .addOptions(new OptionData(OptionType.STRING, "option", "Field to change", true)
                                        .addChoice("Enable NSFW Webhook", "NSFW_WEBHOOK")
                                        .addChoice("Enable Safe Webhook", "SAFE_WEBHOOK")
                                        .addChoice("Enable Reddit", "REDDIT")
                                        .addChoice("Enable Google Search", "GOOGLE")
                                        .addChoice("Enable 4Chan", "4CHAN")
                                        .addChoice("Enable Picsum", "PICSUM")
                                        .addChoice("Enable Rulee34", "RULE34XXX")
                                        .addChoice("Enable Tenor", "TENOR")
                                        .addChoice("Enable Generated Count", "GENERATED_VOICE_CHANNEL"))
                                .addOption(OptionType.STRING, "value", "Value to set", false),
                        new SubcommandData("user", "Change settings for yourself")
                                .addOptions(new OptionData(OptionType.STRING, "option", "Field to change", true)
                                        .addChoice("Set Chances for Images", "chances")
                                        .addChoice("Enable Image Option", "enable")
                                        .addChoice("Disable Image Option", "disable"))
                                .addOption(OptionType.STRING, "value", "Value to set", true),
                        new SubcommandData("send", "Send your configuration to the current channel")
                                .addOption(OptionType.STRING, "description", "Description for configuration", true),
                        new SubcommandData("reset_configuration", "Resets your configuration to default (Chances)"),
                        new SubcommandData("add", "Add something to the bot")
                                .addOptions(new OptionData(OptionType.STRING, "option", "Something to add", true)
                                        .addChoice("Premium", "PREMIUM")
                                        .addChoice("Admin", "ADMIN"))
                                .addOption(OptionType.USER, "value", "Whom to add to", true),
                        new SubcommandData("remove", "Add something to the bot")
                                .addOptions(new OptionData(OptionType.STRING, "option", "Something to add", true)
                                        .addChoice("Premium", "PREMIUM")
                                        .addChoice("Admin", "ADMIN"))
                                .addOption(OptionType.USER, "value", "Whom to add to", true))
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    private static final EmbedBuilder SUCCESS_EMBED = new EmbedBuilder().setTitle("Success").setColor(Color.GREEN);
    private static final EmbedBuilder ERROR_EMBED = new EmbedBuilder().setTitle("Error").setColor(Color.RED);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("config"))
            return;

        // Get the current time and the user's ID
        long now = System.currentTimeMillis();
        long userId = event.getUser().getIdLong();

        // Check if the user is on cooldown
        if (Bot.COOLDOWNS.containsKey(userId) && now - Bot.COOLDOWNS.get(userId) < Bot.COOLDOWN_DURATION) {
            // The user is on cooldown, reply with an embed and return
            errorHandler.sendErrorEmbed(event, "Slow down dude", "Please wait for 2 seconds before using this command again!...");
            return;
        }

        // Update the user's cooldown
        Bot.COOLDOWNS.put(userId, now);

        String subcommand = event.getSubcommandName();
        if (subcommand.equals("bot")) {
            if (!User.get(Main.database, event.getUser().getId()).isAdmin()) {
                errorHandler.sendErrorEmbed(event, "No permission", "Only Bot Admins can use this command");
                return;
            }
            botConfigChange(event, event.getOption("option").getAsString());
        } else if (subcommand.equals("user")) {
            userConfigChange(event);
        } else if (subcommand.equals("send")) {
            sendUserConfig(event);
        } else if (subcommand.equals("reset_configuration")) {
            User user = User.get(Main.database, event.getUser().getId());
            List<ImageOptions> list = ImageOptions.getDefaultOptions();
            ImageOptions[] array = list.toArray(new ImageOptions[list.size()]);
            user.setChances(array);
        } else if (subcommand.equals("add")) {
            if (!User.get(Main.database, event.getUser().getId()).isAdmin()) {
                errorHandler.sendErrorEmbed(event, "No permission", "Only Bot Admins can use this command");
                return;
            }
            add(event);
        } else if (subcommand.equals("remove")) {
            if (!User.get(Main.database, event.getUser().getId()).isAdmin()) {
                errorHandler.sendErrorEmbed(event, "No permission", "Only Bot Admins can use this command");
                return;
            }
            remove(event);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("set-config:"))
            return;

        String pasteId = event.getComponentId().substring("set-config:".length());
        try {
            String configString = Hastebin.getPaste(pasteId);
            updateUserConfig(event, configString);
            event.reply("Your configuration has been updated!").queue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void add(SlashCommandInteractionEvent event) {
        User user = User.get(Main.database, event.getOption("value").getAsUser().getId());
        switch (event.getOption("option").getAsString()) {
            case "PREMIUM":
                user.setPremium(true);
                break;
            case "ADMIN":
                user.setAdmin(true);
        }
    }

    private void remove(SlashCommandInteractionEvent event) {
        User user = User.get(Main.database, event.getOption("value").getAsUser().getId());
        switch (event.getOption("option").getAsString()) {
            case "PREMIUM":
                user.setPremium(false);
                break;
            case "ADMIN":
                user.setAdmin(false);
        }
    }

    private void sendUserConfig(SlashCommandInteractionEvent event) {
        User user = User.get(Main.database, event.getUser().getId());
        Map<String, ImageOptions> allImageOptions = user.getAllImageOptions();

        if (allImageOptions.isEmpty()) {
            sendErrorEmbed(event, "Image options are empty. Please set some values.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ImageOptions> entry : allImageOptions.entrySet()) {
            String imageType = entry.getKey();
            ImageOptions imageOption = entry.getValue();
            sb.append(imageType).append(": enabled=").append(imageOption.isEnabled()).append(", chance=").append(imageOption.getChance()).append("\n");
        }

        String text = event.getOption("description").getAsString();
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("User Configuration")
                .setDescription(text)
                .setColor(Color.GREEN);

        try {
            String pasteId = Hastebin.createPaste(sb.toString());
            event.replyEmbeds(embed.build())
                    .addActionRow(Button.secondary("set-config:" + pasteId, "⚙️ Set Configuration"))
                    .queue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUserConfig(ButtonInteractionEvent event, String configString) {
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
    }

    private void userConfigChange(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("config") || !event.getSubcommandName().equals("user"))
            return;

        String option = event.getOption("option").getAsString();
        String value = event.getOption("value").getAsString();
        User user = User.get(Main.database, event.getUser().getId());

        switch (option) {
            case "chances":
                handleChancesOption(event, user, value);
                break;
            case "enable":
            case "disable":
                handleEnableDisableOption(event, user, value, option.equals("enable"));
                break;
            default:
                sendErrorEmbed(event, "Invalid option: " + option);
                break;
        }
    }

    private void handleChancesOption(SlashCommandInteractionEvent event, User user, String value) {
        String[] chances = value.split(",");
        ImageOptions[] options = new ImageOptions[chances.length];
        for (int i = 0; i < chances.length; i++) {
            String[] chanceData = chances[i].split(":");
            String imageType = chanceData[0];
            if (ImageOptions.getDefaultOptions().stream()
                    .map(ImageOptions::getImageType)
                    .noneMatch(type -> type.equals(imageType))) {
                sendErrorEmbed(event, "Invalid image option: " + imageType);
                return;
            }
            double chance = Double.parseDouble(chanceData[1]);
            options[i] = new ImageOptions(imageType, true, chance);
        }
        user.setChances(options);
        sendSuccessEmbed(event, "Set chances");
    }

    private void handleEnableDisableOption(SlashCommandInteractionEvent event, User user, String value, boolean enable) {
        if (ImageOptions.getDefaultOptions().stream()
                .map(ImageOptions::getImageType)
                .noneMatch(imageType -> imageType.equals(value))) {
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
    }

    private void botConfigChange(SlashCommandInteractionEvent event, String option) {
        String value = event.getOption("value").getAsString();
        boolean enabled = Boolean.parseBoolean(value);

        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            sendErrorEmbed(event, "Incorrect input: only use false or true");
            return;
        }

        switch (option) {
            case "NSFW_WEBHOOK":
            case "SFW_WEBHOOK":
            case "TENOR":
            case "TMDB":
            case "REDDIT":
                if (!Main.checkCredentialsBoolean(Main.env.entries(), "REDDIT", "REDDIT_CLIENT_ID",
                        "REDDIT_CLIENT_SECRET", "REDDIT_USERNAME",
                        "REDDIT_PASSWORD")) {
                    sendErrorEmbed(event, option + " Not found in .env");
                    return;
                }
                break;
            case "GOOGLE":
                if (!Main.checkCredentialsBoolean(Main.env.entries(), "GOOGLE", "GOOGLE_API_KEY", "GOOGLE_CX")) {
                    sendErrorEmbed(event, option + " Not found in .env");
                    return;
                }
                break;
            case "GENERATED_VOICE_CHANNEL":
                if (!Main.containsKey(Main.env.entries(), option)) {
                    sendErrorEmbed(event, option + " Not found in .env");
                    return;
                }
                break;
            default:
                break;
        }

        Bot.config.set(option, enabled);
        sendSuccessEmbed(event, "Set to: " + value);
    }

    private void sendSuccessEmbed(SlashCommandInteractionEvent event, String description) {
        SUCCESS_EMBED.setDescription(description);
        event.replyEmbeds(SUCCESS_EMBED.build()).queue();
    }

    private void sendErrorEmbed(SlashCommandInteractionEvent event, String description) {
        ERROR_EMBED.setDescription(description);
        event.replyEmbeds(ERROR_EMBED.build()).setEphemeral(true).queue();
    }

}