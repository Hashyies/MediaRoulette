package me.hash.mediaroulette.bot.commands;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.User;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

import org.bson.Document;

public class FavoritesCommand extends ListenerAdapter {
    private static final int ITEMS_PER_PAGE = 3;

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("favorites"))
            return;

        event.deferReply().queue();

        // Get the current time and the user's ID
        long now = System.currentTimeMillis();
        long userId = event.getUser().getIdLong();

        // Check if the user is on cooldown
        if (Bot.COOLDOWNS.containsKey(userId) && now - Bot.COOLDOWNS.get(userId) < Bot.COOLDOWN_DURATION) {
            // The user is on cooldown, reply with an embed and return
            Embeds.sendErrorEmbed(event, "Slow down dude", "Please wait for 2 seconds before using this command again!...");
            return;
        }

        // Update the user's cooldown
        Bot.COOLDOWNS.put(userId, now);

        User user = User.get(Main.database, event.getUser().getId());
        if (user.getFavorites().size() == 0 || user.getFavorites() == null) {
            event.getHook().sendMessage("You do not have any favorites yet!").queue();
            return;
        }
        sendPaginator(event.getHook(), user.getFavorites(), 0, true);
    }

    public void sendPaginator(InteractionHook hook, List<Document> favorites, int page, boolean isNewMessage) {
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, favorites.size());

        // If there are no favorites on this page, go to the highest existing page
        if (start >= end) {
            if (favorites.isEmpty()) {
                hook.sendMessage("You do not have any favorites").setEphemeral(true).queue();
                return;
            } else {
                page = (favorites.size() - 1) / ITEMS_PER_PAGE;
                start = page * ITEMS_PER_PAGE;
                end = Math.min(start + ITEMS_PER_PAGE, favorites.size());
            }
        }

        List<Document> favoritesOnPage = favorites.subList(start, end);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("⭐ Your Favorites (Page: " + (page + 1) + ")"); // Added title
        embedBuilder.setAuthor(hook.getInteraction().getUser().getName());
        embedBuilder.setColor(Color.getColor("FFD700")); // GOLD

        List<Button> buttons = new ArrayList<>();

        // Add the "back" button at the beginning if there is a previous page
        if (page > 0) {
            buttons.add(Button.primary("favorite:" + page + ":back", "Back").withEmoji(Emoji.fromUnicode("⬅️"))); // Include
                                                                                                                  // page
                                                                                                                  // number
        }

        // Add the "show" buttons in the middle
        int index = start + 1; // Start numbering from the first item on the page
        for (Document favorite : favoritesOnPage) {
            String id = favorite.getInteger("id").toString();
            embedBuilder.addField(index + ". " + favorite.getString("description"), favorite.getString("image"), false);
            buttons.add(Button.primary("favorite:" + id + ":show:" + index, index + "")); // Include index in button ID
            index++;
        }

        // Add the "next" button at the end if there is a next page
        if (end < favorites.size()) {
            buttons.add(Button.primary("favorite:" + page + ":next", "Next").withEmoji(Emoji.fromUnicode("➡️"))); // Include
                                                                                                                  // page
                                                                                                                  // number
        }

        User user = User.get(Main.database, hook.getInteraction().getUser().getId()); // Get user to access
                                                                                      // getMaxFavorites()
        embedBuilder.setFooter("Favorites: " + favorites.size() + "/" + user.getFavoriteLimit()); // Added footer

        if (isNewMessage) {
            hook.sendMessageEmbeds(embedBuilder.build()).addActionRow(buttons).queue();
        } else {
            hook.editOriginalEmbeds(embedBuilder.build()).setActionRow(buttons).queue();
        }
    }

    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getButton().getId().startsWith("favorite:"))
            return;

        if (!event.getUser().getName().equals(event.getMessage().getEmbeds().get(0).getAuthor().getName())) {
            event.getHook().sendMessage("This is not your menu!").setEphemeral(true).queue();
            return;
        }

        String[] buttonIdParts = event.getButton().getId().split(":");
        String action = buttonIdParts[2];

        event.deferEdit().queue();

        Bot.executor.execute(() -> {
            User user = User.get(Main.database, event.getUser().getId());
            List<Document> favorites = user.getFavorites();

            if (action.equals("delete")) {
                int favoriteId = Integer.parseInt(buttonIdParts[1]) - 1; // In this case, the page number is actually
                                                                         // the favorite ID
                Document favorite = user.getFavorite(favoriteId);
                if (favorite == null) {
                    event.getHook().sendMessage("This favorite does not exist").setEphemeral(true).queue();
                    return;
                }
                user.removeFavorite(favoriteId);
                event.getHook().sendMessage("The favorite has been deleted").setEphemeral(true).queue();
                sendPaginator(event.getHook(), favorites, favoriteId / ITEMS_PER_PAGE, false); // Edit the paginator
            } else if (action.equals("back") || action.equals("next")) {
                int page = Integer.parseInt(buttonIdParts[1]); // Get page number from button ID
                if (action.equals("back")) {
                    sendPaginator(event.getHook(), favorites, Math.max(0, page - 1), false); // Edit the paginator to
                                                                                             // show the previous page
                } else { // action.equals("next")
                    sendPaginator(event.getHook(), favorites,
                            Math.min((favorites.size() - 1) / ITEMS_PER_PAGE, page + 1), false); // Edit the paginator
                                                                                                 // to show the next
                                                                                                 // page
                }
            } else if (action.equals("show")) {
                int index = Integer.parseInt(buttonIdParts[3]); // Get index from button ID
                Document favorite = favorites.get(index - 1); // Adjust for zero-based indexing
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setDescription(favorite.getString("description"));
                embedBuilder.setImage(favorite.getString("image"));
                embedBuilder.setAuthor(event.getUser().getName());
                embedBuilder.setColor(Color.CYAN);
                event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
                int page = Integer.parseInt(buttonIdParts[1]);
                event.getHook().editOriginalComponents(ActionRow.of(
                        Button.danger("favorite:" + index + ":delete", "Delete").withEmoji(Emoji.fromUnicode("❌")), // Added
                                                                                                                    // 'favorite'
                                                                                                                    // at
                                                                                                                    // the
                                                                                                                    // beginning
                        Button.secondary("favorite:" + page + ":back", "Back").withEmoji(Emoji.fromUnicode("⬅️")) // Added
                                                                                                                  // 'favorite'
                                                                                                                  // at
                                                                                                                  // the
                                                                                                                  // beginning
                )).queue();
            }
        });
    }
}
