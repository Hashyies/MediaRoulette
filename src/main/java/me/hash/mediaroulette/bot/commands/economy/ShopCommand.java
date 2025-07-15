package me.hash.mediaroulette.bot.commands.economy;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.ShopItem;
import me.hash.mediaroulette.model.Transaction;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.utils.ShopManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShopCommand extends ListenerAdapter implements CommandHandler {

    private static final Color SHOP_COLOR = new Color(0, 191, 255); // Deep Sky Blue
    private static final int ITEMS_PER_PAGE = 5;
    
    // Store pagination state for users
    private static final Map<Long, ShopSession> SHOP_SESSIONS = new ConcurrentHashMap<>();

    @Override
    public CommandData getCommandData() {
        return Commands.slash("shop", "🛒 Browse and purchase items with your coins")
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("shop")) return;

        event.deferReply().queue();
        Bot.executor.execute(() -> {
            // Get the current time and the user's ID
            long now = System.currentTimeMillis();
            long userId = event.getUser().getIdLong();

            // Check if the user is on cooldown
            if (Bot.COOLDOWNS.containsKey(userId) && now - Bot.COOLDOWNS.get(userId) < Bot.COOLDOWN_DURATION) {
                EmbedBuilder cooldownEmbed = new EmbedBuilder()
                        .setTitle("⏰ Slow Down!")
                        .setDescription("Please wait **2 seconds** before using this command again.")
                        .setColor(new Color(255, 107, 107))
                        .setTimestamp(Instant.now());

                event.getHook().sendMessageEmbeds(cooldownEmbed.build()).queue();
                return;
            }

            // Update the user's cooldown
            Bot.COOLDOWNS.put(userId, now);

            // Get user data
            User user = Main.userService.getOrCreateUser(event.getUser().getId());

            // Create new shop session
            ShopSession session = new ShopSession(user, ShopManager.getInstance().getAvailableItems());
            SHOP_SESSIONS.put(userId, session);

            // Create shop embed
            EmbedBuilder shopEmbed = createShopEmbed(session, event.getUser());
            List<ActionRow> components = createShopComponents(session);

            event.getHook().sendMessageEmbeds(shopEmbed.build())
                    .addComponents(components)
                    .queue();
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("shop:")) return;

        event.deferEdit().queue();
        Bot.executor.execute(() -> {
            long userId = event.getUser().getIdLong();
            ShopSession session = SHOP_SESSIONS.get(userId);
            
            if (session == null) {
                // Session expired, restart
                User user = Main.userService.getOrCreateUser(event.getUser().getId());
                session = new ShopSession(user, ShopManager.getInstance().getAvailableItems());
                SHOP_SESSIONS.put(userId, session);
            }

            String action = event.getComponentId().split(":")[1];

            switch (action) {
                case "prev" -> {
                    session.previousPage();
                    updateShopDisplay(event, session);
                }
                case "next" -> {
                    session.nextPage();
                    updateShopDisplay(event, session);
                }
                case "refresh" -> {
                    session.refreshItems();
                    updateShopDisplay(event, session);
                }
                case "filter" -> {
                    // Filter dropdown will be handled in StringSelectInteraction
                }
                default -> {
                    if (action.startsWith("buy_")) {
                        String itemId = action.substring(4);
                        handlePurchase(event, session, itemId);
                    }
                }
            }
        });
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("shop:filter")) return;

        event.deferEdit().queue();
        Bot.executor.execute(() -> {
            long userId = event.getUser().getIdLong();
            ShopSession session = SHOP_SESSIONS.get(userId);
            
            if (session == null) return;

            String filterValue = event.getValues().get(0);
            session.applyFilter(filterValue);
            updateShopDisplay(event, session);
        });
    }

    private EmbedBuilder createShopEmbed(ShopSession session, net.dv8tion.jda.api.entities.User discordUser) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("🛒 Media Roulette Shop");
        embed.setColor(SHOP_COLOR);
        embed.setTimestamp(Instant.now());

        // Add user avatar
        if (discordUser.getAvatarUrl() != null) {
            embed.setThumbnail(discordUser.getAvatarUrl());
        }

        // User balance
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        embed.setDescription(String.format("💰 **Your Balance:** %s coins\n" +
                "🛍️ **Filter:** %s | **Page:** %d/%d",
                formatter.format(session.getUser().getCoins()),
                session.getCurrentFilter(),
                session.getCurrentPage() + 1,
                session.getTotalPages()));

        // Display items for current page
        List<ShopItem> pageItems = session.getCurrentPageItems();
        
        if (pageItems.isEmpty()) {
            embed.addField("📦 No Items Found", 
                    "```No items match your current filter.\nTry changing the filter or check back later!```", false);
        } else {
            for (int i = 0; i < pageItems.size(); i++) {
                ShopItem item = pageItems.get(i);
                embed.addField(createItemTitle(item), createItemDescription(item, session.getUser()), false);
            }
        }

        // Shop statistics
        embed.addField("📊 Shop Info", 
                String.format("```Total Items: %d\nAvailable: %d\nFiltered Results: %d```",
                        ShopManager.getInstance().getTotalItemCount(),
                        ShopManager.getInstance().getAvailableItemCount(),
                        session.getFilteredItems().size()), true);

        embed.setFooter("💡 Tip: Use filters to find specific items quickly!", null);

        return embed;
    }

    private String createItemTitle(ShopItem item) {
        String stockIndicator = item.isAvailable() ? "" : " ❌";
        return String.format("%s %s%s", item.getEmoji(), item.getName(), stockIndicator);
    }

    private String createItemDescription(ShopItem item, User user) {
        StringBuilder desc = new StringBuilder();
        
        // Description
        desc.append("```").append(item.getDescription()).append("```");
        
        // Price and rarity
        desc.append("**💰 Price:** ").append(item.getFormattedPrice()).append("\n");
        desc.append("**✨ Rarity:** ").append(item.getRarityDisplay()).append("\n");
        
        // Stock info
        if (item.isLimited()) {
            desc.append("**📦 Stock:** ").append(item.getStockDisplay()).append("\n");
        }
        
        // Premium requirement
        if (item.isPremiumOnly() && !user.isPremium()) {
            desc.append("**👑 Premium Required**\n");
        }
        
        // Affordability
        if (!user.canAfford(item.getPrice())) {
            desc.append("**❌ Insufficient Coins**");
        } else if (!item.isAvailable()) {
            desc.append("**❌ Out of Stock**");
        } else if (item.isPremiumOnly() && !user.isPremium()) {
            desc.append("**❌ Premium Only**");
        } else {
            desc.append("**✅ Available for Purchase**");
        }
        
        return desc.toString();
    }

    private List<ActionRow> createShopComponents(ShopSession session) {
        List<ActionRow> components = new ArrayList<>();

        // Filter dropdown
        StringSelectMenu.Builder filterMenu = StringSelectMenu.create("shop:filter")
                .setPlaceholder("🔍 Filter items...")
                .addOption("All Items", "all", "Show all available items")
                .addOption("Themes", "theme", "Cosmetic themes")
                .addOption("Premium", "premium", "Premium upgrades")
                .addOption("Quest Items", "quest", "Quest-related items")
                .addOption("Utilities", "utility", "Useful tools and bonuses")
                .addOption("Limited", "limited", "Limited time items")
                .addOption("Affordable", "affordable", "Items you can afford");

        components.add(ActionRow.of(filterMenu.build()));

        // Navigation and action buttons
        List<Button> buttons = new ArrayList<>();

        // Previous page button
        buttons.add(Button.secondary("shop:prev", "◀ Previous")
                .withDisabled(session.getCurrentPage() == 0));

        // Next page button
        buttons.add(Button.secondary("shop:next", "Next ▶")
                .withDisabled(session.getCurrentPage() >= session.getTotalPages() - 1));

        // Refresh button
        buttons.add(Button.primary("shop:refresh", "🔄 Refresh"));

        // Purchase buttons for current page items
        List<ShopItem> pageItems = session.getCurrentPageItems();
        for (int i = 0; i < Math.min(pageItems.size(), 2); i++) { // Max 2 buy buttons to fit
            ShopItem item = pageItems.get(i);
            if (session.getUser().canAfford(item.getPrice()) && item.isAvailable() && 
                (!item.isPremiumOnly() || session.getUser().isPremium())) {
                buttons.add(Button.success("shop:buy_" + item.getItemId(), 
                        "Buy " + item.getName())
                        .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("💳")));
                break; // Only add one buy button to keep it clean
            }
        }

        components.add(ActionRow.of(buttons));

        return components;
    }

    private void handlePurchase(ButtonInteractionEvent event, ShopSession session, String itemId) {
        ShopItem item = ShopManager.getInstance().getItemById(itemId);
        User user = session.getUser();

        if (item == null) {
            updateShopDisplay(event, session, "❌ Item not found!");
            return;
        }

        if (!item.isAvailable()) {
            updateShopDisplay(event, session, "❌ Item is out of stock!");
            return;
        }

        if (!user.canAfford(item.getPrice())) {
            updateShopDisplay(event, session, "❌ Insufficient coins!");
            return;
        }

        if (item.isPremiumOnly() && !user.isPremium()) {
            updateShopDisplay(event, session, "❌ Premium membership required!");
            return;
        }

        // Process purchase
        Transaction transaction = user.spendCoins(item.getPrice(), Transaction.TransactionType.SHOP_PURCHASE, 
                "Purchased: " + item.getName());
        
        if (transaction != null) {
            ShopManager.getInstance().purchaseItem(itemId);
            Main.userService.updateUser(user);
            
            // Refresh session with updated user data
            session.setUser(Main.userService.getOrCreateUser(user.getUserId()));
            session.refreshItems();
            
            NumberFormat formatter = NumberFormat.getInstance(Locale.US);
            updateShopDisplay(event, session, 
                    String.format("🎉 Successfully purchased **%s** for %s coins!", 
                            item.getName(), formatter.format(item.getPrice())));
        } else {
            updateShopDisplay(event, session, "❌ Purchase failed! Please try again.");
        }
    }

    private void updateShopDisplay(ButtonInteractionEvent event, ShopSession session) {
        updateShopDisplay(event, session, null);
    }

    private void updateShopDisplay(StringSelectInteractionEvent event, ShopSession session) {
        updateShopDisplay(event, session, null);
    }

    private void updateShopDisplay(ButtonInteractionEvent event, ShopSession session, String message) {
        EmbedBuilder shopEmbed = createShopEmbed(session, event.getUser());
        
        if (message != null) {
            shopEmbed.addField("📢 Update", message, false);
        }

        List<ActionRow> components = createShopComponents(session);

        event.getHook().editOriginalEmbeds(shopEmbed.build())
                .setComponents(components)
                .queue();
    }

    private void updateShopDisplay(StringSelectInteractionEvent event, ShopSession session, String message) {
        EmbedBuilder shopEmbed = createShopEmbed(session, event.getUser());
        
        if (message != null) {
            shopEmbed.addField("📢 Update", message, false);
        }

        List<ActionRow> components = createShopComponents(session);

        event.getHook().editOriginalEmbeds(shopEmbed.build())
                .setComponents(components)
                .queue();
    }

    // Shop session class to manage pagination and filtering
    private static class ShopSession {
        private User user;
        private List<ShopItem> allItems;
        private List<ShopItem> filteredItems;
        private int currentPage;
        private String currentFilter;

        public ShopSession(User user, List<ShopItem> items) {
            this.user = user;
            this.allItems = new ArrayList<>(items);
            this.filteredItems = new ArrayList<>(items);
            this.currentPage = 0;
            this.currentFilter = "All Items";
        }

        public void applyFilter(String filter) {
            this.currentFilter = getFilterDisplayName(filter);
            this.currentPage = 0; // Reset to first page

            switch (filter) {
                case "all" -> this.filteredItems = new ArrayList<>(allItems);
                case "theme" -> this.filteredItems = ShopManager.getInstance().getItemsByType(ShopItem.ItemType.THEME);
                case "premium" -> this.filteredItems = ShopManager.getInstance().getItemsByType(ShopItem.ItemType.PREMIUM_UPGRADE);
                case "quest" -> {
                    this.filteredItems = new ArrayList<>();
                    this.filteredItems.addAll(ShopManager.getInstance().getItemsByType(ShopItem.ItemType.QUEST_SKIP));
                    this.filteredItems.addAll(ShopManager.getInstance().getItemsByType(ShopItem.ItemType.COIN_MULTIPLIER));
                }
                case "utility" -> {
                    this.filteredItems = new ArrayList<>();
                    this.filteredItems.addAll(ShopManager.getInstance().getItemsByType(ShopItem.ItemType.FAVORITE_SLOTS));
                    this.filteredItems.addAll(ShopManager.getInstance().getItemsByType(ShopItem.ItemType.DAILY_BONUS));
                }
                case "limited" -> this.filteredItems = ShopManager.getInstance().getLimitedItems();
                case "affordable" -> this.filteredItems = ShopManager.getInstance().getAffordableItems(user.getCoins());
                default -> this.filteredItems = new ArrayList<>(allItems);
            }
        }

        private String getFilterDisplayName(String filter) {
            return switch (filter) {
                case "all" -> "All Items";
                case "theme" -> "Themes";
                case "premium" -> "Premium";
                case "quest" -> "Quest Items";
                case "utility" -> "Utilities";
                case "limited" -> "Limited";
                case "affordable" -> "Affordable";
                default -> "All Items";
            };
        }

        public void refreshItems() {
            this.allItems = ShopManager.getInstance().getAvailableItems();
            applyFilter(getCurrentFilterKey());
        }

        private String getCurrentFilterKey() {
            return switch (currentFilter) {
                case "Themes" -> "theme";
                case "Premium" -> "premium";
                case "Quest Items" -> "quest";
                case "Utilities" -> "utility";
                case "Limited" -> "limited";
                case "Affordable" -> "affordable";
                default -> "all";
            };
        }

        public List<ShopItem> getCurrentPageItems() {
            int start = currentPage * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, filteredItems.size());
            return filteredItems.subList(start, end);
        }

        public void nextPage() {
            if (currentPage < getTotalPages() - 1) {
                currentPage++;
            }
        }

        public void previousPage() {
            if (currentPage > 0) {
                currentPage--;
            }
        }

        public int getTotalPages() {
            return Math.max(1, (int) Math.ceil((double) filteredItems.size() / ITEMS_PER_PAGE));
        }

        // Getters and setters
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
        public List<ShopItem> getFilteredItems() { return filteredItems; }
        public int getCurrentPage() { return currentPage; }
        public String getCurrentFilter() { return currentFilter; }
    }
}