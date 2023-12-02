package me.hash.mediaroulette.bot;

import java.awt.Color;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import me.hash.mediaroulette.utils.random.RandomText;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.interactions.Interaction;


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

    public static void sendImageEmbed(SlashCommandInteractionEvent event, Map<String, String> map, boolean shouldContinue) {
        // Check if the user is on cooldown
        long userId = event.getUser().getIdLong();
        if (Bot.COOLDOWNS.containsKey(userId)
                && System.currentTimeMillis() - Bot.COOLDOWNS.get(userId) < Bot.COOLDOWN_DURATION) {
            // The user is on cooldown, reply with an embed
            event.getHook().sendMessageEmbeds(cooldownEmbed()).setEphemeral(true).queue();
            return;
        }
    
        // Update the user's cooldown
        Bot.COOLDOWNS.put(userId, System.currentTimeMillis());
    
        // Reply with an embed containing the image
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(map.get("title"));
        if (map.containsKey("link"))
                embedBuilder.setUrl(map.get("link"));
        else if (!map.get("image").equals("none") && !map.get("image").startsWith("attachment://"))
                embedBuilder.setUrl(map.get("image"));
        if (!map.get("image").equals("none"))
                embedBuilder.setImage(map.get("image"));
        
        embedBuilder.setColor(Color.CYAN);
        embedBuilder.setDescription(map.get("description"));
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
            Button exit = Button.secondary("exit:" + map.get("type"), "Exit")
                    .withEmoji(Emoji.fromUnicode("❌"));
            WebhookMessageCreateAction<?> action = event.getHook().sendMessageEmbeds(embedBuilder.build())
                    .addActionRow(safe, favorite, nsfw, exit)
                    .addFiles();
                    if (map.getOrDefault("image_type", "null").equals("create"))
                        action.addFiles(FileUpload.fromData(RandomText.generateImage(map.get("image_content")), "image.png"));

                    action.queue();
        } else {
                WebhookMessageCreateAction<?> action = event.getHook().sendMessageEmbeds(embedBuilder.build())
                .addActionRow(safe, favorite, nsfw);
                if (map.getOrDefault("image_type", "null").equals("create")) {
                        byte[] imageData = RandomText.generateImage(map.get("image_content"));
                        action.addFiles(FileUpload.fromData(imageData, "image.png"));
                }
                action.queue();
        }
    
        // Add Image Count to database
        Bot.config.set("image_generated", new BigInteger(Bot.config.getOrDefault("image_generated", "0", String.class))
                .add(new BigInteger(String.valueOf(1))).toString());
    }

    public static void editImageEmbed(ButtonInteractionEvent event, Map<String, String> map) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(map.get("title"));
        if (map.containsKey("link"))
                embedBuilder.setUrl(map.get("link"));
        else if (!map.get("image").equals("none") && !map.get("image").startsWith("attachment://"))
                embedBuilder.setUrl(map.get("image"));
        if (!map.get("image").equals("none"))
                embedBuilder.setImage(map.get("image"));
        
        embedBuilder.setColor(Color.CYAN);
        embedBuilder.setDescription(map.get("description"));
        embedBuilder.setFooter("Current time: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        embedBuilder.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());
        System.out.println("testing");
        Button safe = Button.success("safe:continue", "Safe").withEmoji(Emoji.fromUnicode("✔️"));
        Button favorite = Button.primary("favorite", "Favorite").withEmoji(Emoji.fromUnicode("⭐"));
        Button nsfw = Button.danger("nsfw:continue", "NSFW").withEmoji(Emoji.fromUnicode("🔞"));
        Button end = Button.secondary("exit:" + map.get("type"), "Exit").withEmoji(Emoji.fromUnicode("❌"));
        System.out.println("-------------- " + end.getId());
        MessageEditAction action = event.getMessage().editMessageEmbeds(embedBuilder.build())
                .setActionRow(safe, favorite, nsfw, end);

        if (map.getOrDefault("image_type", "null").equals("create")) {
                byte[] imageData = RandomText.generateImage(map.get("image_content"));
                action.setFiles(FileUpload.fromData(imageData, "image.png"));
        }

        action.queue();
    }

    public static void sendErrorEmbed(Interaction event, String title, String description) {
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


}
