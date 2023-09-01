package me.hash.mediaroulette.bot.commands;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

import java.awt.Color;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.utils.exceptions.InvalidChancesException;
import me.hash.mediaroulette.utils.exceptions.NoEnabledOptionsException;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import club.minnced.discord.webhook.WebhookClient;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class getRandomImage extends ListenerAdapter {

    private String getImage(Interaction event) {
        me.hash.mediaroulette.utils.User user = me.hash.mediaroulette.utils.User.get(Main.database,
                event.getMember().getId());
        String url = null;
        try {
            url = user.getImage();
        } catch (NoEnabledOptionsException | InvalidChancesException e) {
            EmbedBuilder errorEmbed = new EmbedBuilder();
            errorEmbed.setTitle(e instanceof NoEnabledOptionsException ? "No Enabled Options" : "Invalid Chances");
            errorEmbed.setDescription(e.getMessage());
            errorEmbed.setColor(Color.RED);
            if (event instanceof SlashCommandInteractionEvent) {
                ((SlashCommandInteractionEvent) event).getHook().sendMessageEmbeds(errorEmbed.build()).queue();
            } else if (event instanceof ButtonInteractionEvent) {
                ((ButtonInteractionEvent) event).getHook().sendMessageEmbeds(errorEmbed.build()).queue();
            }
        }
        return url;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random"))
            return;
        event.deferReply().queue();
        Bot.executor.execute(() -> {
            boolean shouldContinue = event.getOption("shouldcontinue") != null
                    && event.getOption("shouldcontinue").getAsBoolean();
            String imageUrl = getImage(event);
            if (imageUrl != null) { // Only continue if no exception was thrown
                Embeds.sendImageEmbed(event, "Here is your random image:", imageUrl, shouldContinue);
            }
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] buttonIdParts = event.getButton().getId().split(":");
        String buttonId = buttonIdParts[0];
        boolean shouldContinue = buttonIdParts.length > 1 && "continue".equals(buttonIdParts[1]);
        if (!buttonId.equals("nsfw") && !buttonId.equals("safe") &&
                !buttonId.equals("end"))
            return;
        Bot.executor.execute(() -> {
            // Check if the user who clicked the button is the author of the embed
            if (!event.getUser().getName().equals(event.getMessage().getEmbeds().get(0).getAuthor().getName())) {
                event.reply("This is not your image!").setEphemeral(true).queue();
                return;
            }

            event.getMessage()
                    .editMessageComponents(event.getMessage().getActionRows().stream()
                            .map(actionRow -> ActionRow.of(actionRow.getComponents().stream()
                                    .map(component -> ((Button) component).asDisabled())
                                    .collect(Collectors.toList())))
                            .collect(Collectors.toList()))
                    .queue();

            if (buttonId.equals("end")) {
                event.reply("Ended this session!").setEphemeral(true).queue();
                return;
            }

            if (Bot.config.get("NSFW_WEBHOOK", Boolean.class) && Bot.config.get("SAFE_WEBHOOK", Boolean.class)) {
                String webhookUrl = buttonId.equals("nsfw") ? Main.getEnv("DISCORD_NSFW_WEBHOOK")
                        : Main.getEnv("DISCORD_SAFE_WEBHOOK");
                int color = buttonId.equals("nsfw") ? Color.RED.getRGB() : Color.GREEN.getRGB();

                WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder();
                embedBuilder.setImageUrl(event.getMessage().getEmbeds().get(0).getImage().getUrl());
                embedBuilder.setColor(color);

                WebhookClient client = new WebhookClientBuilder(webhookUrl).build();
                client.send(embedBuilder.build());
            }

            event.reply("Thanks for feedback!").setEphemeral(true).queue();

            // Check if the shouldContinue argument is present and true
            if (shouldContinue) {
                // Generate a new image and update the embed
                String url = getImage(event);
                if (url != null) { // Only continue if no exception was thrown
                    EmbedBuilder newEmbedBuilder = new EmbedBuilder();
                    newEmbedBuilder.setTitle("Here is a random image:");
                    try {
                        newEmbedBuilder.setImage(url);
                        newEmbedBuilder.setUrl(url);
                    } catch (IllegalStateException e) {
                        EmbedBuilder errorEmbedBuilder = new EmbedBuilder();
                        errorEmbedBuilder.setTitle("An error occurred");
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        errorEmbedBuilder.setDescription(sw.toString() + "\nURL: " + url);
                        errorEmbedBuilder.setColor(Color.RED);
                        event.getHook().sendMessageEmbeds(errorEmbedBuilder.build()).queue();
                    }
                    newEmbedBuilder.setColor(Color.CYAN);
                    newEmbedBuilder.setFooter("Current time: "
                            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    newEmbedBuilder.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());
                    Button safe = Button.success("safe:continue", "Safe").withEmoji(Emoji.fromUnicode("✔️"));
                    Button nsfw = Button.danger("nsfw:continue", "NSFW").withEmoji(Emoji.fromUnicode("🔞"));
                    Button end = Button.secondary("end", "End").withEmoji(Emoji.fromUnicode("❌"));
                    event.getMessage().editMessageEmbeds(newEmbedBuilder.build()).setActionRow(safe, nsfw, end).queue();

                    Bot.config.set("image_generated",
                            new BigInteger(Bot.config.getOrDefault("image_generated", "0", String.class))
                                    .add(new BigInteger(String.valueOf(1))).toString());
                }
            }
        });
    }

}
