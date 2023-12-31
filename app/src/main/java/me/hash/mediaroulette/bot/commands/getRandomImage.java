package me.hash.mediaroulette.bot.commands;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class getRandomImage extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random"))
            return;

        event.deferReply().queue();
        Bot.executor.execute(() -> {
            if (!event.getChannelType().equals(ChannelType.PRIVATE) &&
                    (!event.getChannelType().equals(ChannelType.TEXT)
                            || !event.getGuildChannel().asTextChannel().isNSFW())) {
                Embeds.sendErrorEmbed(event, "Not an NSFW channel!",
                        "This channel must be set as NSFW or the message must be set in DMs!");
                return;
            }

            boolean shouldContinue = event.getOption("shouldcontinue") != null
                    && event.getOption("shouldcontinue").getAsBoolean();
            String subcommand = event.getSubcommandName();
            String option = null;
            try {
                option = event.getOptions().get(0).getAsString();
                if (event.getOptions().get(0).getName().equals("shouldcontinue")) {
                    option = null;
                }
            } catch (IndexOutOfBoundsException e) {
                // Option was not provided, so it remains null
            }

            // Declare new final variables
            final String finalOption = option;

            ImageSource.fromName(subcommand.toUpperCase()).ifPresent(source -> {
                Map<String, String> image = source.handle(event, shouldContinue, finalOption);
                while (image.get("image") == null)
                    image = source.handle(event, shouldContinue, finalOption);
                if (image.get("image").equals("end"))
                    return;
                    image.put("type", subcommand.toUpperCase());
                if (image.get("image") != null) {
                    Embeds.sendImageEmbed(event, image, shouldContinue);
                } else {
                    Embeds.sendErrorEmbed(event, "Error", "This subcommand is not recognized");
                }
            });
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] buttonIdParts = event.getButton().getId().split(":");
        String buttonId = buttonIdParts[0];
        boolean shouldContinue = buttonIdParts.length > 1 && "continue".equals(buttonIdParts[1]);

        if (!buttonId.equals("nsfw") && !buttonId.equals("safe") && !buttonId.startsWith("exit:")
                && !buttonId.equals("favorite") && event.getButton().getId().startsWith("favorite:"))
            return;

        if (event.getButton().getId().startsWith("favorite:"))
            return;

        event.deferEdit().queue();

        Bot.executor.execute(() -> {
            // Check if the user who clicked the button is the author of the embed
            if (!event.getUser().getName().equals(event.getMessage().getEmbeds().get(0).getAuthor().getName())) {
                event.getHook().sendMessage("This is not your image!").setEphemeral(true).queue();
                return;
            }

            // Handle favorite button click
            if (buttonId.equals("favorite")) {
                User user = User.get(Main.database, event.getUser().getId());
                user.addFavorite(event.getMessage().getEmbeds().get(0).getDescription(),
                        event.getMessage().getEmbeds().get(0).getImage().getUrl(), "image");

                List<Button> buttons = event.getMessage().getButtons();
                List<Button> disabledButtons = new ArrayList<>();
                for (Button button : buttons) {
                    if (button.getId().equals("favorite"))
                        disabledButtons.add(button.asDisabled());
                    else
                        disabledButtons.add(button);
                }

                event.getMessage().editMessageComponents(ActionRow.of(disabledButtons)).queue();
                return;
            }

            // Send message to webhook
            if (Bot.config.get("NSFW_WEBHOOK", Boolean.class) && Bot.config.get("SAFE_WEBHOOK", Boolean.class)) {
                String webhookUrl = buttonId.equals("nsfw") ? Main.getEnv("DISCORD_NSFW_WEBHOOK")
                        : Main.getEnv("DISCORD_SAFE_WEBHOOK");
                int color = buttonId.equals("nsfw") ? Color.RED.getRGB() : Color.GREEN.getRGB();

                WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder();
                try {
                    embedBuilder.setImageUrl(event.getMessage().getEmbeds().get(0).getImage().getUrl());
                } catch (NullPointerException e) {
                    event.getHook()
                            .sendMessage("It seems that the image did not load... The error messsage has been saved!")
                            .queue();
                    embedBuilder.setDescription(e.getMessage());
                }
                embedBuilder.setColor(color);

                WebhookClient client = new WebhookClientBuilder(webhookUrl).build();
                client.send(embedBuilder.build());
            }

            // Disable all buttons
            List<Button> buttons = event.getMessage().getButtons();
            List<Button> disabledButtons = new ArrayList<>();
            for (Button button : buttons) {
                disabledButtons.add(button.asDisabled());
            }

            event.getMessage().editMessageComponents(ActionRow.of(disabledButtons)).queue();

            // Handle continue button click
            if (shouldContinue) {
                String subcommand = event.getMessage().getButtonsByLabel("Exit", true).get(0).getId().split(":")[1];
System.out.println(subcommand);
                ImageSource.fromName(subcommand.toUpperCase()).ifPresent(source -> {

                    String option = null;
                    String description = event.getMessage().getEmbeds().get(0).getDescription();
                    if (description != null) {
                        if (description.split("\n").length < 2)
                            option = null;
                        else {
                            String[] parts = description.split("\n")[1].split(":");
                            if (parts.length >= 2
                                    && Arrays.asList("🔎 Query", "🔎 Board", "🔎 Subreddit").contains(parts[0])) {
                                option = parts[1].trim();
                            }
                        }
                    }

                    Map<String, String> image = source.handle(event, true, option);
                    while (image.get("image") == null)
                        image = source.handle(event, shouldContinue, option);
                    image.put("type", subcommand.toUpperCase());
                    if (image.get("image").equals("end"))
                        return;
                        
                    if (image.get("image") != null) {
                        Embeds.editImageEmbed(event, image);
                    } else {
                        Embeds.sendErrorEmbed(event, "Error",
                                "This subcommand is not recognized (Or the image you encountered was null... You were using: "
                                        + source + ")");
                    }
                });
            }
        });
    }
}