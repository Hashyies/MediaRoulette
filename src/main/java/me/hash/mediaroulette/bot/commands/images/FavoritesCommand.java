package me.hash.mediaroulette.bot.commands.images;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.errorHandler;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.Favorite;
import me.hash.mediaroulette.model.User;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.InteractionHook;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.interactions.components.ActionRow;

// TODO: make locales here
public class FavoritesCommand extends ListenerAdapter implements CommandHandler {
    private static final int ITEMS_PER_PAGE = 25;

    @Override
    public CommandData getCommandData() {
        return Commands.slash("favorites", "Shows your favorites")
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
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
            errorHandler.sendErrorEmbed(event, "Slow down!", "Please wait for 2 seconds before using this command again.");
            return;
        }

        // Update the user's cooldown
        Bot.COOLDOWNS.put(userId, now);


        User user = Main.userService.getOrCreateUser(event.getUser().getId());
        if (user.getFavorites() == null || user.getFavorites().isEmpty()) {
            event.getHook().sendMessage("You do not have any favorites yet!").queue();
            return;
        }

        // Show the first favorite by default
        sendFavoriteDetail(event.getHook(), user, 0, true);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("favorite:"))
            return;

        // Check if the user is the same as the one who initiated the interaction
        String originalUserId = event.getMessage().getInteractionMetadata().getUser().getId();
        if (!event.getUser().getId().equals(originalUserId)) {
            event.reply("This is not your menu!").setEphemeral(true).queue();
            return;
        }

        // Split the button ID
        String[] parts = event.getComponentId().split(":");
        String action = parts[1];

        event.deferEdit().queue();

        // Use the service layer to get or create the user
        User user = Main.userService.getOrCreateUser(event.getUser().getId());
        List<Favorite> favorites = user.getFavorites();

        switch (action) {
            case "prev":
            case "next": {
                int page = Integer.parseInt(parts[2]);
                int newPage = action.equals("prev") ? page - 1 : page + 1;
                sendFavoriteDetail(event.getHook(), user, newPage * ITEMS_PER_PAGE, false);
                break;
            }
            case "delete": {
                int index = Integer.parseInt(parts[2]);
                if (index < 0 || index >= favorites.size()) {
                    event.getHook().sendMessage("Invalid favorite to delete.").setEphemeral(true).queue();
                    return;
                }
                // Remove the favorite using the updated User model
                user.removeFavorite(index);
                // Persist the change via the service (if your service automatically saves changes,
                // ensure removeFavorite internally calls updateUser or call Main.userService.updateUser(user) here)
                Main.userService.updateUser(user);

                event.getHook().sendMessage("Favorite has been deleted.").setEphemeral(true).queue();

                // Refresh the favorites list
                favorites = user.getFavorites();

                // After deletion, if there are favorites left, show the adjusted favorite details.
                if (!favorites.isEmpty()) {
                    int newIndex = Math.min(index, favorites.size() - 1);
                    sendFavoriteDetail(event.getHook(), user, newIndex, false);
                } else {
                    // No favorites left, inform the user
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                    .setTitle("No Favorites")
                                    .setDescription("You have no favorites left.")
                                    .setColor(Color.RED)
                                    .build())
                            .setComponents()
                            .queue();
                }
                break;
            }
            // Handle other actions if necessary
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("favorite-select-menu"))
            return;

        // Check if the user is the same as the one who initiated the interaction
        String originalUserId = event.getMessage().getInteractionMetadata().getUser().getId();
        if (!event.getUser().getId().equals(originalUserId)) {
            event.reply("This is not your menu!").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();

        int selectedIndex = Integer.parseInt(event.getValues().getFirst());

        User user = Main.userService.getOrCreateUser(event.getUser().getId());
        sendFavoriteDetail(event.getHook(), user, selectedIndex, false);
    }

    private void sendFavoriteDetail(InteractionHook hook, User user, int index, boolean isNewMessage) {
        List<Favorite> favorites = user.getFavorites();

        if (index < 0 || index >= favorites.size()) {
            hook.sendMessage("Invalid favorite selected.").setEphemeral(true).queue();
            return;
        }

        Favorite favorite = favorites.get(index);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("⭐ Favorite Details");
        embedBuilder.setDescription(favorite.getDescription());
        embedBuilder.setImage(favorite.getImage());
        embedBuilder.setColor(Color.CYAN);
        embedBuilder.setFooter("Favorite " + (index + 1) + "/" + favorites.size());

        int currentPage = index / ITEMS_PER_PAGE;

        // Build the selection menu and navigation buttons
        List<ActionRow> paginatorComponents = buildPaginatorComponents(user, currentPage);

        // Add a delete button
        Button deleteButton = Button.danger("favorite:delete:" + index, "Delete")
                .withEmoji(Emoji.fromUnicode("❌"));
        ActionRow deleteButtonRow = ActionRow.of(deleteButton);

        // Assemble components
        // First, add the selection menu and navigation buttons
        List<ActionRow> actionRows = new ArrayList<>(paginatorComponents);
        // Then, add the delete button below them
        actionRows.add(deleteButtonRow);

        if (isNewMessage) {
            hook.sendMessageEmbeds(embedBuilder.build())
                    .setComponents(actionRows)
                    .queue();
        } else {
            hook.editOriginalEmbeds(embedBuilder.build())
                    .setComponents(actionRows)
                    .queue();
        }
    }

    private List<ActionRow> buildPaginatorComponents(User user, int page) {
        List<Favorite> favorites = user.getFavorites();

        int totalPages = (int) Math.ceil((double) favorites.size() / ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1)); // Ensure page is within bounds

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, favorites.size());

        List<Favorite> favoritesOnPage = favorites.subList(start, end);

        // Build the selection menu with options from the current page
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("favorite-select-menu")
                .setPlaceholder("Select a favorite to view details")
                .setMinValues(1)
                .setMaxValues(1);

        int index = start; // Zero-based index
        for (Favorite favorite : favoritesOnPage) {
            String description = favorite.getDescription();
            // Remove any line breaks and limit the description length
            description = description.replaceAll("\\n", " ");
            description = (description.length() > 80) ? description.substring(0, 80) + "..." : description;

            String label = (index + 1) + ". " + description;
            // Ensure the label does not exceed 100 characters
            if (label.length() > 100) {
                label = label.substring(0, 97) + "...";
            }
            String value = String.valueOf(index); // Use zero-based index for value

            menuBuilder.addOption(label, value);
            index++;
        }

        // Assemble the selection menu into an ActionRow
        List<ActionRow> actionRows = new ArrayList<>();
        actionRows.add(ActionRow.of(menuBuilder.build()));
        return actionRows;
    }


}
