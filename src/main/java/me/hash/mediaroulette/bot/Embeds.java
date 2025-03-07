package me.hash.mediaroulette.bot;

import me.hash.mediaroulette.utils.media.ImageGenerator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Embeds {

    public static CompletableFuture<Message> sendImageEmbed(Interaction event, Map<String, String> map, boolean shouldContinue) {
        EmbedBuilder embedBuilder = createEmbed(event, map);
        MessageEmbed messageEmbed = embedBuilder.build();

        // Create buttons
        List<Button> buttons = getButtons(shouldContinue);

        // Handle "create" type (generating images)
        if ("create".equals(map.get("image_type"))) {
            byte[] imageBytes = ImageGenerator.generateImage(map.get("image_content"));
            FileUpload fileUpload = FileUpload.fromData(imageBytes, "image.png");

            embedBuilder.setImage("attachment://image.png");
            messageEmbed = embedBuilder.build();

            return handleImageEmbedWithFile(event, messageEmbed, fileUpload, buttons);
        } else {
            return handleImageEmbed(event, messageEmbed, buttons);
        }
    }

    private static CompletableFuture<Message> handleImageEmbed(Interaction event, MessageEmbed embed, List<Button> buttons) {
        CompletableFuture<Message> futureMessage = new CompletableFuture<>();

        // Determine appropriate handling based on the interaction type
        if (event instanceof SlashCommandInteraction slashEvent) {
            // Sending the embed
            slashEvent.getHook().sendMessageEmbeds(embed)
                    .addActionRow(buttons)
                    .queue(
                            message -> futureMessage.complete(message),
                            futureMessage::completeExceptionally
                    );
        } else if (event instanceof ButtonInteraction buttonEvent) {
            buttonEvent.getHook().sendMessageEmbeds(embed)
                    .addActionRow(buttons)
                    .queue(
                            message -> futureMessage.complete(message),
                            futureMessage::completeExceptionally
                    );
        } else {
            System.out.println("Unsupported Interaction type: " + event.getClass().getName());
            futureMessage.completeExceptionally(new IllegalArgumentException("Unsupported interaction type"));
        }

        return futureMessage;
    }

    private static CompletableFuture<Message> handleImageEmbedWithFile(Interaction event, MessageEmbed embed, FileUpload file, List<Button> buttons) {
        CompletableFuture<Message> futureMessage = new CompletableFuture<>();

        // Handling sending message with a file
        if (event instanceof SlashCommandInteraction slashEvent) {
            slashEvent.getHook().sendMessageEmbeds(embed)
                    .addFiles(file)
                    .addActionRow(buttons)
                    .queue(
                            message -> futureMessage.complete(message),
                            futureMessage::completeExceptionally
                    );
        } else if (event instanceof ButtonInteraction buttonEvent) {
            buttonEvent.getHook().sendMessageEmbeds(embed)
                    .addFiles(file)
                    .addActionRow(buttons)
                    .queue(
                            message -> futureMessage.complete(message),
                            futureMessage::completeExceptionally
                    );
        } else {
            System.out.println("Unsupported Interaction type: " + event.getClass().getName());
            futureMessage.completeExceptionally(new IllegalArgumentException("Unsupported interaction type"));
        }

        return futureMessage;
    }

    public static void editImageEmbed(ButtonInteractionEvent event, Map<String, String> map) {
        EmbedBuilder embedBuilder = createEmbed(event, map); // Create embed based on new data
        MessageEmbed messageEmbed = embedBuilder.build();

        // Create buttons for the edited response
        List<Button> buttons = getButtons(true);

        if ("create".equals(map.get("image_type"))) {
            byte[] imageBytes = ImageGenerator.generateImage(map.get("image_content"));
            FileUpload fileUpload = FileUpload.fromData(imageBytes, "image.png");

            event.getMessage().editMessage(MessageEditData.fromEmbeds(messageEmbed))
                    .setFiles(fileUpload)
                    .setActionRow(buttons)
                    .queue(); // Edit the message directly
        } else {
            event.getMessage().editMessage(MessageEditData.fromEmbeds(messageEmbed))
                    .setActionRow(buttons)
                    .queue();
        }
    }

    private static List<Button> getButtons(boolean shouldContinue) {
        Button safe = Button.success("safe" + (shouldContinue ? ":continue" : ""), "Safe").withEmoji(Emoji.fromUnicode("✔️"));
        Button nsfw = Button.danger("nsfw" + (shouldContinue ? ":continue" : ""), "NSFW").withEmoji(Emoji.fromUnicode("🔞"));
        Button favorite = Button.primary("favorite", "Favorite").withEmoji(Emoji.fromUnicode("⭐"));

        List<Button> buttons = new ArrayList<>(Arrays.asList(safe, favorite, nsfw));

        if (shouldContinue) {
            Button exit = Button.secondary("exit", "Exit").withEmoji(Emoji.fromUnicode("❌"));
            buttons.add(exit);
        }

        return buttons;
    }

    public static void disableButton(ButtonInteractionEvent event, String buttonId) {
        List<ActionRow> updatedActionRows = new ArrayList<>();

        for (ActionRow actionRow : event.getMessage().getActionRows()) {
            List<ItemComponent> updatedComponents = new ArrayList<>();
            for (ItemComponent component : actionRow.getComponents()) {
                if (component.getType() == Component.Type.BUTTON) {
                    Button button = (Button) component;
                    if (Objects.equals(button.getId(), buttonId)) {
                        button = button.asDisabled();
                    }

                    updatedComponents.add(button);
                } else {
                    updatedComponents.add(component);
                }
            }
            updatedActionRows.add(ActionRow.of(updatedComponents));
        }

        event.getMessage().editMessageComponents(updatedActionRows).queue();
    }

    public static void disableAllButtons(ButtonInteractionEvent event) {
        event.getMessage().editMessageComponents(event.getMessage().getActionRows().stream()
                        .map(ActionRow::asDisabled)
                        .collect(Collectors.toList()))
                .queue();
    }

    private static EmbedBuilder createEmbed(Interaction event, Map<String, String> map) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(map.get("title"));

        if (map.containsKey("link"))
            embedBuilder.setUrl(map.get("link"));
        else if (!"none".equals(map.get("image")) && !map.get("image").startsWith("attachment://"))
            embedBuilder.setUrl(map.get("image"));

        if (!"none".equals(map.get("image")))
            embedBuilder.setImage(map.get("image"));

        embedBuilder.setColor(Color.CYAN);
        embedBuilder.setDescription(map.get("description"));
        embedBuilder.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());

        return embedBuilder;
    }

}
