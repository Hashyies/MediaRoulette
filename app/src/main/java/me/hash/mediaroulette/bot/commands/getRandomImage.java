package me.hash.mediaroulette.bot.commands;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

import java.awt.Color;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.utils.RandomImage;
import net.dv8tion.jda.api.EmbedBuilder;
import club.minnced.discord.webhook.WebhookClient;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class getRandomImage extends ListenerAdapter {

    private static final Map<Long, Long> COOLDOWNS = new HashMap<>();
    private static final long COOLDOWN_DURATION = 2500; // 2.5 seconds in milliseconds
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public String getImage() {
        // Define the probability of each method being selected
        int prob4Chan = 20;
        int probPicsum = 10;
        int probImgur = 35;
        // int probReddit = 35;

        // Generate a random number between 0 and 100
        int rand = new Random().nextInt(100);

        // Select a method based on the random number and the probabilities
        String result = "";
        if (rand < prob4Chan) {
            // Method a returns a string in an array, so we only want the first string
            result = RandomImage.get4ChanImage()[0];
        } else if (rand < prob4Chan + probPicsum) {
            result = RandomImage.getPicSumImage();
        } else if (rand < prob4Chan + probPicsum + probImgur) {
            result = RandomImage.getImgurImage();
        } else {
            try {
                result = RandomImage.getRandomReddit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result != null ? result : getImage();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random"))
            return;
        event.deferReply().queue();
        executor.execute(() -> {
            // Check if the user is on cooldown
            long userId = event.getUser().getIdLong();
            if (COOLDOWNS.containsKey(userId)
                    && System.currentTimeMillis() - COOLDOWNS.get(userId) < COOLDOWN_DURATION) {
                // The user is on cooldown, reply with an embed
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Slow down dude...");
                embedBuilder.setDescription(
                        "Please wait for " + COOLDOWN_DURATION / 1000 + " seconds before using this command again.");
                embedBuilder.setColor(Color.RED);
                event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                return;
            }

            // Update the user's cooldown
            COOLDOWNS.put(userId, System.currentTimeMillis());

            String url = getImage();

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Here is a random image:");
            embedBuilder.setImage(url);
            embedBuilder.setUrl(url);
            embedBuilder.setColor(Color.CYAN);
            embedBuilder.setFooter("Current time: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            User user = event.getUser();
            embedBuilder.setAuthor(user.getName(), null, user.getEffectiveAvatarUrl());

            // Check if the shouldContinue option is present and true
            boolean shouldContinue = event.getOption("shouldContinue") != null
                    && event.getOption("shouldContinue").getAsBoolean();

            Button safe = Button.primary(shouldContinue ? "safe:continue" : "safe", "Safe")
                    .withEmoji(Emoji.fromUnicode("✔️"));
            Button nsfw = Button.danger(shouldContinue ? "nsfw:continue" : "nsfw", "NSFW")
                    .withEmoji(Emoji.fromUnicode("🔞"));

            event.getHook().sendMessageEmbeds(embedBuilder.build()).addActionRow(safe, nsfw).queue();
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] buttonIdParts = event.getButton().getId().split(":");
        String buttonId = buttonIdParts[0];
        boolean shouldContinue = buttonIdParts.length > 1 && "continue".equals(buttonIdParts[1]);
        if (!buttonId.equals("nsfw") && !buttonId.equals("safe"))
            return;
        executor.execute(() -> {
            // Check if the user who clicked the button is the author of the embed
            if (!event.getUser().getName().equals(event.getMessage().getEmbeds().get(0).getAuthor().getName())) {
                // The user is not the author of the embed, reply with "This is not your image!"
                event.reply("This is not your image!").setEphemeral(true).queue();
                return;
            }

            String webhookUrl = buttonId.equals("nsfw") ? Main.getEnv("DISCORD_NSFW_WEBHOOK")
                    : Main.getEnv("DISCORD_SAFE_WEBHOOK");
            int color = buttonId.equals("nsfw") ? Color.RED.getRGB() : Color.GREEN.getRGB();

            WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder();
            embedBuilder.setImageUrl(event.getMessage().getEmbeds().get(0).getImage().getUrl());
            embedBuilder.setColor(color);

            WebhookClient client = new WebhookClientBuilder(webhookUrl).build();
            client.send(embedBuilder.build());

            event.reply("Thanks for feedback!").setEphemeral(true).queue();

            // Check if the shouldContinue argument is present and true

            if (shouldContinue) {
                // Generate a new image and update the embed
                String url = getImage();
                EmbedBuilder newEmbedBuilder = new EmbedBuilder();
                newEmbedBuilder.setTitle("Here is a random image:");
                newEmbedBuilder.setImage(url);
                newEmbedBuilder.setUrl(url);
                newEmbedBuilder.setColor(Color.CYAN);
                newEmbedBuilder.setFooter("Current time: "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                User user = event.getUser();
                newEmbedBuilder.setAuthor(user.getName(), null, user.getEffectiveAvatarUrl());
                Button safe = Button.primary("safe", "Safe").withEmoji(Emoji.fromUnicode("✔️"));
                Button nsfw = Button.danger("nsfw", "NSFW").withEmoji(Emoji.fromUnicode("🔞"));
                event.getMessage().editMessageEmbeds(newEmbedBuilder.build()).setActionRow(safe, nsfw).queue();
            } else {
                // Disable all buttons on the message
                event.getMessage()
                        .editMessageComponents(event.getMessage().getActionRows().stream()
                                .map(actionRow -> ActionRow.of(actionRow.getComponents().stream()
                                        .map(component -> ((Button) component).asDisabled())
                                        .collect(Collectors.toList())))
                                .collect(Collectors.toList()))
                        .queue();
            }
        });
    }

}
