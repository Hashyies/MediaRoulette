package me.hash.mediaroulette.bot.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

import java.awt.Color;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.utils.RandomImage;
import me.hash.mediaroulette.utils.random.RandomReddit;
import me.hash.mediaroulette.bot.Bot;
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

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public String getImage() {
        // Define the probability of each method being selected
        int prob4Chan = 15;
        int probPicsum = 5;
        int probImgur = 25;
        int probReddit = 30;
        // int Rule34xxx = 25;

        // Generate a random number between 0 and 100
        int rand = new Random().nextInt(100);

        // Select a method based on the random number and the probabilities
        String result = "";
        if (rand < prob4Chan) {
            // Method a returns a string in an array, so we only want the first string
            result = RandomImage.get4ChanImage(null)[0];
        } else if (rand < prob4Chan + probPicsum) {
            result = RandomImage.getPicSumImage();
        } else if (rand < prob4Chan + probPicsum + probImgur) {
            result = RandomImage.getImgurImage();
        } else if (rand < prob4Chan + probPicsum + probImgur + probReddit) {
            try {
                result = RandomReddit.getRandomReddit(null);
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            result = RandomImage.getRandomRule34xxx();
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
            if (Bot.COOLDOWNS.containsKey(userId)
                    && System.currentTimeMillis() - Bot.COOLDOWNS.get(userId) < Bot.COOLDOWN_DURATION) {
                // The user is on cooldown, reply with an embed
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Slow down dude...");
                embedBuilder.setDescription(
                        "Please wait for " + Bot.COOLDOWN_DURATION / 1000
                                + " seconds before using this command again.");
                embedBuilder.setColor(Color.RED);
                event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                return;
            }

            // Update the user's cooldown
            Bot.COOLDOWNS.put(userId, System.currentTimeMillis());

            String url = getImage();

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Here is a random image:");
            try {
                embedBuilder.setImage(url);
                embedBuilder.setUrl(url);
            } catch (IllegalStateException e) {
                // An error occurred, display an error message with the stack trace in a red
                // embed
                EmbedBuilder errorEmbedBuilder = new EmbedBuilder();
                errorEmbedBuilder.setTitle("An error occurred");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                errorEmbedBuilder.setDescription(sw.toString() + "\nURL: " + url);
                errorEmbedBuilder.setColor(Color.RED);
                event.getHook().sendMessageEmbeds(errorEmbedBuilder.build()).queue();
            }
            embedBuilder.setColor(Color.CYAN);
            embedBuilder.setFooter("Current time: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            User user = event.getUser();
            embedBuilder.setAuthor(user.getName(), null, user.getEffectiveAvatarUrl());

            // Check if the shouldContinue option is present and true
            boolean shouldContinue = event.getOption("shouldcontinue") != null
                    && event.getOption("shouldcontinue").getAsBoolean();

            Button safe = Button.success(shouldContinue ? "safe:continue" : "safe", "Safe")
                    .withEmoji(Emoji.fromUnicode("✔️"));
            Button nsfw = Button.danger(shouldContinue ? "nsfw:continue" : "nsfw", "NSFW")
                    .withEmoji(Emoji.fromUnicode("🔞"));
            Button end = Button.secondary("end", "End")
                    .withEmoji(Emoji.fromUnicode("❌"));

            ActionRow actionRow = ActionRow.of(safe, nsfw);
            if (shouldContinue) {
                actionRow = ActionRow.of(safe, nsfw, end);
            }
            event.getHook().sendMessageEmbeds(embedBuilder.build())
                    .addActionRow(actionRow.getComponents())
                    .queue();

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
        executor.execute(() -> {
            // Check if the user who clicked the button is the author of the embed
            if (!event.getUser().getName().equals(event.getMessage().getEmbeds().get(0).getAuthor().getName())) {
                // The user is not the author of the embed, reply with "This is not your image!"
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

            String webhookUrl = buttonId.equals("nsfw") ? Main.getEnv("DISCORD_NSFW_WEBHOOK")
                    : Main.getEnv("DISCORD_SAFE_WEBHOOK");
            int color = buttonId.equals("nsfw") ? Color.RED.getRGB() : Color.GREEN.getRGB();

            WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder();
            embedBuilder.setImageUrl(event.getMessage().getEmbeds().get(0).getImage().getUrl());
            embedBuilder.setColor(color);

            WebhookClient client = new WebhookClientBuilder(webhookUrl).build();
            client.send(embedBuilder.build());

            event.reply("Thanks for feedback!").setEphemeral(true).queue();

            // Disable all buttons

            // Check if the shouldContinue argument is present and tru
            if (shouldContinue) {
                // Generate a new image and update the embed
                String url = getImage();
                EmbedBuilder newEmbedBuilder = new EmbedBuilder();
                newEmbedBuilder.setTitle("Here is a random image:");
                try {
                    newEmbedBuilder.setImage(url);
                    newEmbedBuilder.setUrl(url);
                } catch (IllegalStateException e) {
                    // An error occurred, display an error message with the stack trace in a red
                    // embed
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
                User user = event.getUser();
                newEmbedBuilder.setAuthor(user.getName(), null, user.getEffectiveAvatarUrl());
                Button safe = Button.success("safe:continue", "Safe").withEmoji(Emoji.fromUnicode("✔️"));
                Button nsfw = Button.danger("nsfw:continue", "NSFW").withEmoji(Emoji.fromUnicode("🔞"));
                Button end = Button.secondary("end", "End").withEmoji(Emoji.fromUnicode("❌"));
                event.getMessage().editMessageEmbeds(newEmbedBuilder.build()).setActionRow(safe, nsfw, end).queue();
            }
        });
    }

}