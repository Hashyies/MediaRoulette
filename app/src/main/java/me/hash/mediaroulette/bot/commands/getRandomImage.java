package me.hash.mediaroulette.bot.commands;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import club.minnced.discord.webhook.WebhookClient;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class getRandomImage extends ListenerAdapter {
    // TODO: MAKE IMAGE SOURCE REPLY WITH FULL EMBEDS INSTEAD OF URLS
    private void sendErrorEmbed(Interaction event, String title, String description) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle(title);
        errorEmbed.setDescription(description);
        errorEmbed.setColor(Color.RED);
        if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).getHook().sendMessageEmbeds(errorEmbed.build()).queue();
        } else if (event instanceof ButtonInteractionEvent) {
            ((ButtonInteractionEvent) event).getHook().sendMessageEmbeds(errorEmbed.build()).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random"))
            return;
        event.deferReply().queue();
        Bot.executor.execute(() -> {
            boolean shouldContinue = event.getOption("shouldcontinue") != null
                    && event.getOption("shouldcontinue").getAsBoolean();

            String subcommand = event.getSubcommandName();
            String option = event.getOption(subcommand) != null ? event.getOption(subcommand).getAsString() : null;
            ImageSource source = ImageSource.fromName(subcommand.toUpperCase());
            if (source != null) {
                String imageUrl = source.handle(event, shouldContinue, option);
                if (imageUrl != null) {
                    Embeds.sendImageEmbed(event, "Here is your random " + subcommand + " image:", imageUrl,
                            shouldContinue);
                } else {
                    sendErrorEmbed(event, "Error", "This subcommand is not recognized");
                }
            }
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] buttonIdParts = event.getButton().getId().split(":");
        String buttonId = buttonIdParts[0];
        boolean shouldContinue = buttonIdParts.length > 1 && "continue".equals(buttonIdParts[1]);
        if (!buttonId.equals("nsfw") && !buttonId.equals("safe") &&
                !buttonId.equals("end") && !buttonId.equals("favorite"))
            return;
            event.deferEdit().queue();

        Bot.executor.execute(() -> {
            // Check if the user who clicked the button is the author of the embed
            if (!event.getUser().getName().equals(event.getMessage().getEmbeds().get(0).getAuthor().getName())) {
                event.reply("This is not your image!").setEphemeral(true).queue();
                return;
            }

            // Handle favorite button click
            if (buttonId.equals("favorite")) {
                // Add to favorites
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
                    event.getMessage().reply("It seems that the image did not load... The error messsage has been saved!").queue();
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
                String subcommand = event.getMessage().getEmbeds().get(0).getTitle().split(" ")[4];
                ImageSource source = ImageSource.fromName(subcommand.toUpperCase());
                if (source != null) {
                    String imageUrl = source.handle(event, true, null);
                    Embeds.editImageEmbed(event, "Here is your random " + subcommand + " image:", imageUrl);
                } else {
                    sendErrorEmbed(event, "Error", "This subcommand is not recognized"
                            + "(Or the image you encountered was null... You were using: " + source + ")");
                }
                return;
            }
        });
    }

}