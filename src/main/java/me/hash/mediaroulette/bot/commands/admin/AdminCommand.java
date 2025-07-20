package me.hash.mediaroulette.bot.commands.admin;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.BotInventoryItem;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.service.BotInventoryService;
import me.hash.mediaroulette.service.GiveawayService;
import me.hash.mediaroulette.utils.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.Color;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AdminCommand extends ListenerAdapter implements CommandHandler {

    private static final Color PRIMARY_COLOR = new Color(88, 101, 242);
    private static final Color SUCCESS_COLOR = new Color(87, 242, 135);
    private static final Color ERROR_COLOR = new Color(220, 53, 69);
    private static final Color ADMIN_COLOR = new Color(255, 215, 0);

    private final BotInventoryService botInventoryService;
    private final GiveawayService giveawayService;
    private final ConcurrentHashMap<String, String> temporaryStorage;

    public AdminCommand() {
        this.botInventoryService = new BotInventoryService();
        this.giveawayService = new GiveawayService();
        this.temporaryStorage = new ConcurrentHashMap<>();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("admin", "Admin commands for bot management")
                .addSubcommands(
                        new SubcommandData("additem", "Add an item to bot inventory")
                                .addOption(OptionType.STRING, "type", "Item type", true)
                                .addOption(OptionType.STRING, "name", "Item name", true)
                                .addOption(OptionType.STRING, "description", "Item description", true)
                                .addOption(OptionType.STRING, "rarity", "Item rarity (common/rare/epic/legendary)", true)
                                .addOption(OptionType.STRING, "value", "Item value (for non-nitro items)", false)
                                .addOption(OptionType.INTEGER, "expires_days", "Days until expiration (for nitro)", false),
                        new SubcommandData("viewinventory", "View bot inventory"),
                        new SubcommandData("removeitem", "Remove an item from bot inventory")
                                .addOption(OptionType.STRING, "item_id", "Item ID to remove", true),
                        new SubcommandData("givecoins", "Give coins to a user")
                                .addOption(OptionType.USER, "user", "User to give coins to", true)
                                .addOption(OptionType.INTEGER, "amount", "Amount of coins", true)
                                .addOption(OptionType.STRING, "reason", "Reason for giving coins", false),
                        new SubcommandData("setpremium", "Set user premium status")
                                .addOption(OptionType.USER, "user", "User to modify", true)
                                .addOption(OptionType.BOOLEAN, "premium", "Premium status", true),
                        new SubcommandData("stats", "View comprehensive admin statistics"),
                        new SubcommandData("cleanup", "Clean up expired items and old data"),
                        new SubcommandData("maintenance", "Toggle maintenance mode")
                                .addOption(OptionType.BOOLEAN, "enabled", "Enable maintenance mode", true)
                );
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("admin")) return;

        User user = Main.userService.getOrCreateUser(event.getUser().getId());
        // Check if user is admin
        if (!user.isAdmin()) {
            sendError(event, "Access Denied - You don't have permission to use admin commands.");
            return;
        }

        String subcommand = event.getSubcommandName();
        switch (subcommand) {
            case "additem" -> handleAddItem(event);
            case "viewinventory" -> handleViewInventory(event);
            case "removeitem" -> handleRemoveItem(event);
            case "givecoins" -> handleGiveCoins(event);
            case "setpremium" -> handleSetPremium(event);
            case "stats" -> handleStats(event);
            case "cleanup" -> handleCleanup(event);
            case "maintenance" -> handleMaintenance(event);
            default -> sendError(event, "Unknown admin subcommand.");
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().startsWith("admin:addnitro:")) {
            handleNitroModal(event);
        }
    }

    private void handleAddItem(SlashCommandInteractionEvent event) {
        String type = event.getOption("type").getAsString();
        String name = event.getOption("name").getAsString();
        String description = event.getOption("description").getAsString();
        String rarity = event.getOption("rarity").getAsString();
        String value = event.getOption("value") != null ? event.getOption("value").getAsString() : null;
        Integer expiresDays = event.getOption("expires_days") != null ? event.getOption("expires_days").getAsInt() : null;

        if (!List.of("discord_nitro", "premium_voucher", "boost", "coins", "special").contains(type)) {
            sendError(event, "Invalid type. Use: discord_nitro, premium_voucher, boost, coins, special");
            return;
        }

        // Special handling for Discord Nitro - show modal for secure link input
        if ("discord_nitro".equals(type)) {
            String modalId = "admin:addnitro:" + System.currentTimeMillis();
            
            TextInput linkInput = TextInput.create("nitro_link", "Discord Nitro Gift Link", TextInputStyle.SHORT)
                    .setPlaceholder("https://discord.gift/...")
                    .setRequired(true)
                    .setMaxLength(200)
                    .build();

            Modal modal = Modal.create(modalId, "Add Discord Nitro Gift")
                    .addComponents(ActionRow.of(linkInput))
                    .build();

            // Store item data in temporary storage for retrieval
            String itemData = String.join("|", name, description, rarity, 
                String.valueOf(expiresDays != null ? expiresDays : 30));
            
            event.replyModal(modal).queue();
            temporaryStorage.put(modalId, itemData);
            return;
        }

        // Create regular item
        BotInventoryItem item = new BotInventoryItem();
        item.setName(name);
        item.setDescription(description);
        item.setType(type);
        item.setRarity(rarity);
        item.setValue(value != null ? value : "N/A");
        
        if (expiresDays != null) {
            item.setExpiresAt(Instant.now().plus(expiresDays, ChronoUnit.DAYS));
        }

        botInventoryService.addItem(item);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Item Added to Bot Inventory")
                .setDescription(String.format("**%s** has been added to the bot inventory!", name))
                .addField("Type", type, true)
                .addField("Rarity", rarity, true)
                .addField("ID", "`" + item.getId() + "`", true)
                .setColor(SUCCESS_COLOR)
                .setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void handleNitroModal(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        String itemData = temporaryStorage.get(modalId);
        
        if (itemData == null) {
            sendModalError(event, "Session expired. Please try again.");
            return;
        }

        String[] parts = itemData.split("\\|");
        String name = parts[0];
        String description = parts[1];
        String rarity = parts[2];
        int expiresDays = Integer.parseInt(parts[3]);

        String nitroLink = event.getValue("nitro_link").getAsString();
        
        if (!nitroLink.startsWith("https://discord.gift/")) {
            sendModalError(event, "Invalid Discord gift link format.");
            return;
        }

        BotInventoryItem item = new BotInventoryItem();
        item.setName(name);
        item.setDescription(description);
        item.setType("discord_nitro");
        item.setRarity(rarity);
        item.setValue(nitroLink);
        item.setExpiresAt(Instant.now().plus(expiresDays, ChronoUnit.DAYS));

        botInventoryService.addItem(item);
        temporaryStorage.remove(modalId);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Discord Nitro Added to Bot Inventory")
                .setDescription(String.format("**%s** has been securely added to the bot inventory!", name))
                .addField("Type", "Discord Nitro", true)
                .addField("Rarity", rarity, true)
                .addField("Expires", String.format("<t:%d:R>", item.getExpiresAt().getEpochSecond()), true)
                .addField("ID", "`" + item.getId() + "`", false)
                .setColor(SUCCESS_COLOR)
                .setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void handleViewInventory(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        
        Bot.executor.submit(() -> {
            List<BotInventoryItem> items = botInventoryService.getAllItems();
            
            if (items.isEmpty()) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Bot Inventory")
                        .setDescription("The bot inventory is empty.")
                        .setColor(PRIMARY_COLOR)
                        .setTimestamp(Instant.now());
                event.getHook().sendMessageEmbeds(embed.build()).queue();
                return;
            }

            StringBuilder description = new StringBuilder();
            int activeCount = 0;
            int expiredCount = 0;
            
            for (BotInventoryItem item : items) {
                if (item.isExpired()) {
                    expiredCount++;
                    description.append(String.format("~~**%s** (%s) - `%s` [EXPIRED]~~\n", 
                        item.getName(), item.getRarity(), item.getId()));
                } else {
                    activeCount++;
                    description.append(String.format("**%s** (%s) - `%s`\n", 
                        item.getName(), item.getRarity(), item.getId()));
                }
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Bot Inventory")
                    .setDescription(description.toString())
                    .addField("Active Items", String.valueOf(activeCount), true)
                    .addField("Expired Items", String.valueOf(expiredCount), true)
                    .addField("Total Items", String.valueOf(items.size()), true)
                    .setColor(ADMIN_COLOR)
                    .setTimestamp(Instant.now());

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }

    private void handleRemoveItem(SlashCommandInteractionEvent event) {
        String itemId = event.getOption("item_id").getAsString();
        
        event.deferReply(true).queue();
        
        Bot.executor.submit(() -> {
            boolean removed = botInventoryService.removeItem(itemId);
            
            if (removed) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Item Removed")
                        .setDescription(String.format("Item with ID `%s` has been removed from bot inventory.", itemId))
                        .setColor(SUCCESS_COLOR)
                        .setTimestamp(Instant.now());
                event.getHook().sendMessageEmbeds(embed.build()).queue();
            } else {
                sendError(event, "Item not found: " + itemId);
            }
        });
    }

    private void handleGiveCoins(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.User targetUser = event.getOption("user").getAsUser();
        int amount = event.getOption("amount").getAsInt();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "Admin gift";
        
        event.deferReply(true).queue();
        
        Bot.executor.submit(() -> {
            try {
                User user = Main.userService.getOrCreateUser(targetUser.getId());
                user.setCoins(user.getCoins() + amount);
                Main.userService.updateUser(user);
                
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Coins Given")
                        .setDescription(String.format("Given **%,d coins** to %s", amount, targetUser.getAsMention()))
                        .addField("Reason", reason, false)
                        .addField("New Balance", String.format("%,d coins", user.getCoins()), false)
                        .setColor(SUCCESS_COLOR)
                        .setTimestamp(Instant.now());
                
                event.getHook().sendMessageEmbeds(embed.build()).queue();
                
            } catch (Exception e) {
                sendError(event, "Failed to give coins: " + e.getMessage());
            }
        });
    }

    private void handleSetPremium(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.User targetUser = event.getOption("user").getAsUser();
        boolean premium = event.getOption("premium").getAsBoolean();
        
        event.deferReply(true).queue();
        
        Bot.executor.submit(() -> {
            try {
                User user = Main.userService.getOrCreateUser(targetUser.getId());
                user.setPremium(premium);
                Main.userService.updateUser(user);
                
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Premium Status Updated")
                        .setDescription(String.format("Set premium status for %s to **%s**", 
                            targetUser.getAsMention(), premium ? "ENABLED" : "DISABLED"))
                        .setColor(premium ? SUCCESS_COLOR : ERROR_COLOR)
                        .setTimestamp(Instant.now());
                
                event.getHook().sendMessageEmbeds(embed.build()).queue();
                
            } catch (Exception e) {
                sendError(event, "Failed to update premium status: " + e.getMessage());
            }
        });
    }

    private void handleStats(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        
        Bot.executor.submit(() -> {
            long totalUsers = Main.userService.getTotalUsers();
            long totalImages = Main.userService.getTotalImagesGenerated();
            List<BotInventoryItem> botItems = botInventoryService.getAllItems();
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Comprehensive Admin Statistics")
                    .setColor(ADMIN_COLOR)
                    .setTimestamp(Instant.now());

            // User statistics
            embed.addField("Users", String.format("%,d total users", totalUsers), true);
            embed.addField("Images", String.format("%,d total generated", totalImages), true);
            embed.addField("Bot Items", String.format("%d total items", botItems.size()), true);

            // Item breakdown
            long activeItems = botItems.stream().filter(BotInventoryItem::canBeGivenAway).count();
            long expiredItems = botItems.stream().filter(BotInventoryItem::isExpired).count();
            long nitroItems = botItems.stream().filter(i -> "discord_nitro".equals(i.getType())).count();
            
            embed.addField("Active Items", String.valueOf(activeItems), true);
            embed.addField("Expired Items", String.valueOf(expiredItems), true);
            embed.addField("Nitro Items", String.valueOf(nitroItems), true);

            // Bot statistics
            embed.addField("Guilds", String.valueOf(Bot.getShardManager().getGuilds().size()), true);
            embed.addField("Shards", String.valueOf(Bot.getShardManager().getShards().size()), true);
            embed.addField("Uptime", getUptimeString(), true);

            // Giveaway statistics
            int activeGiveaways = giveawayService.getActiveGiveaways().size();
            embed.addField("Active Giveaways", String.valueOf(activeGiveaways), true);
            embed.addField("Cache Size", String.valueOf(temporaryStorage.size()), true);
            embed.addField("Memory Usage", getMemoryUsage(), true);

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }

    private void handleCleanup(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        
        Bot.executor.submit(() -> {
            try {
                // Clean up expired items
                List<BotInventoryItem> expiredItems = botInventoryService.getAllItems().stream()
                    .filter(BotInventoryItem::isExpired)
                    .toList();
                
                int removedItems = 0;
                for (BotInventoryItem item : expiredItems) {
                    if (botInventoryService.removeItem(item.getId())) {
                        removedItems++;
                    }
                }
                
                // Clean up completed giveaways older than 30 days
                int cleanedGiveaways = giveawayService.cleanupOldGiveaways(30);
                
                // Clean up temporary storage entries older than 1 hour
                long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
                temporaryStorage.entrySet().removeIf(entry -> {
                    try {
                        String[] parts = entry.getKey().split(":");
                        if (parts.length >= 3) {
                            long timestamp = Long.parseLong(parts[2]);
                            return timestamp < oneHourAgo;
                        }
                    } catch (NumberFormatException ignored) {}
                    return false;
                });
                
                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Cleanup Complete")
                    .setColor(SUCCESS_COLOR)
                    .setDescription(String.format(
                        "**Cleanup Results:**\n" +
                        "• Removed %d expired items\n" +
                        "• Cleaned %d old giveaways\n" +
                        "• Cleared temporary cache",
                        removedItems, cleanedGiveaways))
                    .setTimestamp(Instant.now());
                
                event.getHook().sendMessageEmbeds(embed.build()).queue();
                
            } catch (Exception e) {
                sendError(event, "Failed to perform cleanup: " + e.getMessage());
            }
        });
    }

    private void handleMaintenance(SlashCommandInteractionEvent event) {
        boolean enabled = event.getOption("enabled").getAsBoolean();
        
        event.deferReply(true).queue();
        
        Bot.executor.submit(() -> {
            try {
                String status = enabled ? "MAINTENANCE" : "ONLINE";
                String activity = enabled ? "Under Maintenance" : "Use /support for help! | Alpha :3";
                
                // Update bot status across all shards
                Bot.getShardManager().getShards().forEach(jda -> {
                    if (enabled) {
                        jda.getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.watching("Under Maintenance"));
                        jda.getPresence().setStatus(net.dv8tion.jda.api.OnlineStatus.DO_NOT_DISTURB);
                    } else {
                        jda.getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.playing("Use /support for help! | Alpha :3"));
                        jda.getPresence().setStatus(net.dv8tion.jda.api.OnlineStatus.ONLINE);
                    }
                });
                
                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(enabled ? "Maintenance Mode Enabled" : "Maintenance Mode Disabled")
                    .setColor(enabled ? ERROR_COLOR : SUCCESS_COLOR)
                    .setDescription(String.format(
                        "**Status:** %s\n" +
                        "**Activity:** %s\n" +
                        "**Applied to:** %d shards",
                        status, activity, Bot.getShardManager().getShards().size()))
                    .setTimestamp(Instant.now());
                
                event.getHook().sendMessageEmbeds(embed.build()).queue();
                
            } catch (Exception e) {
                sendError(event, "Failed to toggle maintenance mode: " + e.getMessage());
            }
        });
    }

    private String getUptimeString() {
        long uptime = System.currentTimeMillis() - Bot.getShardManager().getShards().get(0).getGatewayPing();
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else {
            return String.format("%dm %ds", minutes, seconds % 60);
        }
    }

    private String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long total = runtime.totalMemory();
        return String.format("%.1f/%.1f MB", used / 1024.0 / 1024.0, total / 1024.0 / 1024.0);
    }

    private void sendError(SlashCommandInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Error")
                .setDescription(message)
                .setColor(ERROR_COLOR)
                .setTimestamp(Instant.now());
        
        if (event.isAcknowledged()) {
            event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
        } else {
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
    }

    private void sendModalError(ModalInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Error")
                .setDescription(message)
                .setColor(ERROR_COLOR)
                .setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}