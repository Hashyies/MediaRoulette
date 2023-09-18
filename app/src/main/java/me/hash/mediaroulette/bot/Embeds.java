package me.hash.mediaroulette.bot;

import java.awt.Color;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class Embeds {

    public static MessageEmbed cooldownEmbed() {
        // The user is on cooldown, reply with an embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Slow down dude...");
        embedBuilder.setDescription(
                "Please wait for " + Bot.COOLDOWN_DURATION / 1000 + " seconds before using this command again.");
        embedBuilder.setColor(Color.RED);
        return embedBuilder.build();
    }

    public static void sendImageEmbed(SlashCommandInteractionEvent event, String title, String imageUrl, boolean shouldContinue) {
        // Check if the user is on cooldown
        long userId = event.getUser().getIdLong();
        if (Bot.COOLDOWNS.containsKey(userId)
                && System.currentTimeMillis() - Bot.COOLDOWNS.get(userId) < Bot.COOLDOWN_DURATION) {
            // The user is on cooldown, reply with an embed
            event.replyEmbeds(cooldownEmbed()).setEphemeral(true).queue();
            return;
        }
    
        // Update the user's cooldown
        Bot.COOLDOWNS.put(userId, System.currentTimeMillis());
    
        // Reply with an embed containing the image
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setUrl(imageUrl);
        embedBuilder.setImage(imageUrl);
        embedBuilder.setColor(Color.CYAN);
        embedBuilder.setFooter("Current time: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        User user = event.getUser();
        embedBuilder.setAuthor(user.getName(), null, user.getEffectiveAvatarUrl());
    
        Button safe;
        Button nsfw;
        Button favorite = Button.primary("favorite", "Favorite").withEmoji(Emoji.fromUnicode("⭐"));
        if (shouldContinue) {
            safe = Button.success("safe:continue", "Safe")
                    .withEmoji(Emoji.fromUnicode("✔️"));
            nsfw = Button.danger("nsfw:continue", "NSFW")
                    .withEmoji(Emoji.fromUnicode("🔞"));
        } else {
            safe = Button.success("safe", "Safe")
                    .withEmoji(Emoji.fromUnicode("✔️"));
            nsfw = Button.danger("nsfw", "NSFW")
                    .withEmoji(Emoji.fromUnicode("🔞"));
        }
    
        if (shouldContinue) {
            Button exit = Button.secondary("exit", "Exit")
                    .withEmoji(Emoji.fromUnicode("❌"));
            event.getHook().sendMessageEmbeds(embedBuilder.build())
                    .addActionRow(safe, favorite, nsfw, exit)
                    .queue();
        } else {
            event.getHook().sendMessageEmbeds(embedBuilder.build())
                    .addActionRow(safe, favorite, nsfw)
                    .queue();
        }
    
        // Add Image Count to database
        Bot.config.set("image_generated", new BigInteger(Bot.config.getOrDefault("image_generated", "0", String.class))
                .add(new BigInteger(String.valueOf(1))).toString());
    }

    public static void editImageEmbed(ButtonInteractionEvent event, String title, String imageUrl) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setImage(imageUrl);
        embedBuilder.setUrl(imageUrl);
        embedBuilder.setColor(Color.CYAN);
        embedBuilder.setFooter("Current time: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        embedBuilder.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());

        Button safe = Button.success("safe:continue", "Safe").withEmoji(Emoji.fromUnicode("✔️"));
        Button favorite = Button.primary("favorite", "Favorite").withEmoji(Emoji.fromUnicode("⭐"));
        Button nsfw = Button.danger("nsfw:continue", "NSFW").withEmoji(Emoji.fromUnicode("🔞"));
        Button end = Button.secondary("end", "End").withEmoji(Emoji.fromUnicode("❌"));

        event.getMessage().editMessageEmbeds(embedBuilder.build())
                .setActionRow(safe, favorite, nsfw, end)
                .queue();
    }


}
