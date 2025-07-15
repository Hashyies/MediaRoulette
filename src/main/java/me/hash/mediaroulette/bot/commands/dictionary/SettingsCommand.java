package me.hash.mediaroulette.bot.commands.dictionary;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.Dictionary;
import me.hash.mediaroulette.model.DictionaryAssignment;
import me.hash.mediaroulette.model.ImageOptions;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.service.DictionaryService;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SettingsCommand extends ListenerAdapter implements CommandHandler {
    private static final Color PRIMARY_COLOR = new Color(88, 101, 242);
    private static final Color SUCCESS_COLOR = new Color(87, 242, 135);
    private static final Color ERROR_COLOR = new Color(220, 53, 69);
    
    private static final List<String> SUPPORTED_SOURCES = Arrays.asList(
        "tenor", "google", "reddit"
    );
    
    private DictionaryService dictionaryService;
    
    public SettingsCommand(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash("settings", "⚙️ Configure dictionary assignments for sources")
                .addSubcommands(
                    new SubcommandData("assign", "Assign a dictionary to a source")
                        .addOption(OptionType.STRING, "source", "Source name (tenor, reddit, etc.)", true)
                        .addOption(OptionType.STRING, "dictionary", "Dictionary ID", true),
                    new SubcommandData("view", "View current assignments"),
                    new SubcommandData("unassign", "Remove dictionary assignment")
                        .addOption(OptionType.STRING, "source", "Source name", true),
                    new SubcommandData("shareconfig", "Share your configuration via Hastebin")
                        .addOption(OptionType.STRING, "title", "Title for the configuration share", false)
                        .addOption(OptionType.STRING, "description", "Description for the configuration share", false)
                );
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("settings")) return;
        
        event.deferReply().queue();
        Bot.executor.execute(() -> {
            String subcommand = event.getSubcommandName();
            String userId = event.getUser().getId();
            
            switch (subcommand) {
                case "assign" -> handleAssign(event, userId);
                case "view" -> handleView(event, userId);
                case "unassign" -> handleUnassign(event, userId);
                case "shareconfig" -> handleShareConfig(event, userId);
            }
        });
    }
    
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        
        if (componentId.startsWith("settings:setconfig:")) {
            event.deferReply(true).queue(); // Ephemeral response
            Bot.executor.execute(() -> handleApplyConfig(event, componentId));
        }
    }
    
    private void handleAssign(SlashCommandInteractionEvent event, String userId) {
        String source = event.getOption("source").getAsString().toLowerCase();
        String dictionaryId = event.getOption("dictionary").getAsString();
        
        if (!SUPPORTED_SOURCES.contains(source)) {
            sendError(event, "Unsupported source. Supported: " + String.join(", ", SUPPORTED_SOURCES));
            return;
        }
        
        Optional<Dictionary> dictOpt = dictionaryService.getDictionary(dictionaryId);
        if (dictOpt.isEmpty() || !dictOpt.get().canBeViewedBy(userId)) {
            sendError(event, "Dictionary not found or access denied.");
            return;
        }
        
        dictionaryService.assignDictionary(userId, source, dictionaryId);
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("✅ Assignment Complete")
            .setDescription(String.format("Dictionary **%s** assigned to **%s**", 
                dictOpt.get().getName(), source))
            .setColor(SUCCESS_COLOR);
            
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void handleView(SlashCommandInteractionEvent event, String userId) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("⚙️ Dictionary Assignments")
            .setColor(PRIMARY_COLOR);
            
        StringBuilder sb = new StringBuilder();
        boolean hasAssignments = false;
        
        for (String source : SUPPORTED_SOURCES) {
            Optional<String> assignedDict = dictionaryService.getAssignedDictionary(userId, source);
            if (assignedDict.isPresent()) {
                Optional<Dictionary> dict = dictionaryService.getDictionary(assignedDict.get());
                if (dict.isPresent()) {
                    sb.append(String.format("**%s**: %s (`%s`)\n", 
                        formatSourceName(source), dict.get().getName(), dict.get().getId()));
                    hasAssignments = true;
                }
            } else {
                sb.append(String.format("**%s**: *Default dictionary*\n", formatSourceName(source)));
            }
        }
        
        if (!hasAssignments) {
            embed.setDescription("No custom dictionary assignments. All sources use default dictionaries.");
        } else {
            embed.setDescription(sb.toString());
        }
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void handleUnassign(SlashCommandInteractionEvent event, String userId) {
        String source = event.getOption("source").getAsString().toLowerCase();
        
        if (!SUPPORTED_SOURCES.contains(source)) {
            sendError(event, "Unsupported source.");
            return;
        }
        
        // Remove the assignment
        if (dictionaryService.unassignDictionary(userId, source)) {
            sendSuccess(event, String.format("Dictionary assignment removed from **%s**. Now using default.", 
                formatSourceName(source)));
        } else {
            sendError(event, String.format("No dictionary assigned to **%s**.", formatSourceName(source)));
        }
    }
    
    private void handleShareConfig(SlashCommandInteractionEvent event, String userId) {
        try {
            User user = Main.userService.getOrCreateUser(userId);
            
            // Get optional title and description from command options
            String customTitle = event.getOption("title") != null ? event.getOption("title").getAsString() : null;
            String customDescription = event.getOption("description") != null ? event.getOption("description").getAsString() : null;
            
            StringBuilder configBuilder = new StringBuilder();
            
            configBuilder.append("=".repeat(50)).append("\n");
            if (customTitle != null) {
                configBuilder.append("📋 ").append(customTitle.toUpperCase()).append("\n");
            } else {
                configBuilder.append("📋 MEDIAROULETTE CONFIGURATION EXPORT\n");
            }
            configBuilder.append("=".repeat(50)).append("\n");
            configBuilder.append("User ID: ").append(userId).append("\n");
            configBuilder.append("Export Date: ").append(java.time.Instant.now().toString()).append("\n");
            if (customDescription != null) {
                configBuilder.append("Description: ").append(customDescription).append("\n");
            }
            configBuilder.append("\n");
            
            // Dictionary Assignments
            configBuilder.append("🎯 DICTIONARY ASSIGNMENTS\n");
            configBuilder.append("-".repeat(30)).append("\n");
            
            boolean hasAssignments = false;
            for (String source : SUPPORTED_SOURCES) {
                Optional<String> assignedDict = dictionaryService.getAssignedDictionary(userId, source);
                if (assignedDict.isPresent()) {
                    Optional<Dictionary> dict = dictionaryService.getDictionary(assignedDict.get());
                    if (dict.isPresent()) {
                        configBuilder.append(String.format("%-10s: %s (%s)\n", 
                            formatSourceName(source), dict.get().getName(), dict.get().getId()));
                        hasAssignments = true;
                    }
                }
            }
            
            if (!hasAssignments) {
                configBuilder.append("No custom dictionary assignments (using defaults)\n");
            }
            
            // User's Dictionaries
            configBuilder.append("\n📚 YOUR DICTIONARIES\n");
            configBuilder.append("-".repeat(30)).append("\n");
            
            List<Dictionary> userDictionaries = dictionaryService.getUserDictionaries(userId);
            if (userDictionaries.isEmpty()) {
                configBuilder.append("No custom dictionaries created\n");
            } else {
                for (Dictionary dict : userDictionaries) {
                    configBuilder.append(String.format("📖 %s (%s)\n", dict.getName(), dict.getId()));
                    configBuilder.append(String.format("   Description: %s\n", dict.getDescription()));
                    configBuilder.append(String.format("   Words: %d | Public: %s | Usage: %d\n", 
                        dict.getWordCount(), dict.isPublic() ? "Yes" : "No", dict.getUsageCount()));
                    
                    if (dict.getWordCount() > 0) {
                        configBuilder.append("   Content: ");
                        List<String> words = dict.getWords();
                        if (words.size() <= 10) {
                            configBuilder.append(String.join(", ", words));
                        } else {
                            configBuilder.append(String.join(", ", words.subList(0, 10)))
                                .append("... (").append(words.size() - 10).append(" more)");
                        }
                        configBuilder.append("\n");
                    }
                    configBuilder.append("\n");
                }
            }
            
            // Source Chances Configuration
            configBuilder.append("🎲 SOURCE CHANCES CONFIGURATION\n");
            configBuilder.append("-".repeat(30)).append("\n");
            
            Map<String, ImageOptions> imageOptions = user.getImageOptionsMap();
            if (imageOptions.isEmpty()) {
                configBuilder.append("Using default source chances\n");
            } else {
                configBuilder.append(String.format("%-15s %-10s %-10s\n", "Source", "Enabled", "Chance %"));
                configBuilder.append("-".repeat(35)).append("\n");
                
                for (Map.Entry<String, ImageOptions> entry : imageOptions.entrySet()) {
                    ImageOptions option = entry.getValue();
                    String sourceName = entry.getKey();
                    try {
                        sourceName = formatSourceName(entry.getKey());
                    } catch (Exception e) {
                        // Use original key if formatting fails
                    }
                    configBuilder.append(String.format("%-15s %-10s %-10.1f\n", 
                        sourceName,
                        option.isEnabled() ? "✅ Yes" : "❌ No",
                        option.getChance()));
                }
            }
            
            // Usage Statistics
            configBuilder.append("\n📊 USAGE STATISTICS\n");
            configBuilder.append("-".repeat(30)).append("\n");
            configBuilder.append(String.format("Images Generated: %d\n", user.getImagesGenerated()));
            configBuilder.append(String.format("Favorites Saved: %d\n", user.getFavorites().size()));
            configBuilder.append(String.format("NSFW Enabled: %s\n", user.isNsfw() ? "Yes" : "No"));
            configBuilder.append(String.format("Premium User: %s\n", user.isPremium() ? "Yes" : "No"));
            configBuilder.append(String.format("Locale: %s\n", user.getLocale()));
            configBuilder.append(String.format("Theme: %s\n", user.getTheme()));
            
            configBuilder.append("\n").append("=".repeat(50)).append("\n");
            configBuilder.append("💡 To import this configuration, use /settings import <hastebin-url>\n");
            configBuilder.append("🆘 Need help? Use /support to join our Discord server\n");
            configBuilder.append("=".repeat(50));
            
            // Store configuration in database and create shareable button
            String configId = storeConfigInDatabase(configBuilder.toString(), customTitle, customDescription, userId);
            
            if (configId != null) {
                String titleText = customTitle != null ? customTitle : "Configuration Share";
                String descText = customDescription != null ? customDescription : "MediaRoulette configuration";
                
                // Create a button that other users can click to apply this configuration
                Button applyConfigButton = Button.primary("settings:setconfig:" + configId, "📥 Apply This Configuration");
                
                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📤 Configuration Shared Successfully!")
                    .setDescription("Your configuration is ready to share!")
                    .setColor(SUCCESS_COLOR)
                    .addField("📋 Title", titleText, true)
                    .addField("📝 Description", descText, true)
                    .setFooter("Configuration created at", null)
                    .setTimestamp(java.time.Instant.now());
                    
                event.getHook().sendMessageEmbeds(embed.build())
                    .addComponents(ActionRow.of(applyConfigButton))
                    .queue();
            } else {
                sendError(event, "Failed to save configuration. Please try again later.");
            }
            
        } catch (Exception e) {
            sendError(event, "Failed to generate configuration: " + e.getMessage());
        }
    }
    
    private void handleApplyConfig(ButtonInteractionEvent event, String componentId) {
        try {
            // Extract config ID from component ID: "settings:setconfig:configId"
            String configId = componentId.substring("settings:setconfig:".length());
            String userId = event.getUser().getId();
            
            // Get the configuration content from database
            Document configDoc = getConfigFromDatabase(configId);
            if (configDoc == null) {
                throw new Exception("Configuration not found or expired");
            }
            
            String configContent = configDoc.getString("content");
            String configTitle = configDoc.getString("title");
            String configDescription = configDoc.getString("description");
            
            // Parse and apply the configuration to the user who clicked the button
            parseAndApplyConfig(userId, configContent);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Configuration Applied Successfully!")
                .setDescription("The shared configuration has been applied to your account.")
                .setColor(SUCCESS_COLOR)
                .addField("📋 Applied Config", configTitle != null ? configTitle : "Configuration", true)
                .addField("📝 Description", configDescription != null ? configDescription : "MediaRoulette configuration", true)
                .setFooter("Configuration applied at", null)
                .setTimestamp(java.time.Instant.now());
                
            event.getHook().sendMessageEmbeds(embed.build()).queue();
            
        } catch (Exception e) {
            System.err.println("Failed to apply shared configuration: " + e.getMessage());
            e.printStackTrace();
            
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Configuration Apply Failed")
                .setDescription("Failed to apply the shared configuration. The configuration may be invalid or expired.")
                .setColor(ERROR_COLOR)
                .setTimestamp(java.time.Instant.now());
                
            event.getHook().sendMessageEmbeds(errorEmbed.build()).queue();
        }
    }
    
    private String storeConfigInDatabase(String configContent, String title, String description, String creatorUserId) {
        try {
            MongoCollection<Document> configCollection = Main.database.getCollection("shared_configs");
            
            String configId = UUID.randomUUID().toString();
            
            Document configDoc = new Document()
                .append("_id", configId)
                .append("content", configContent)
                .append("title", title)
                .append("description", description)
                .append("creatorUserId", creatorUserId)
                .append("createdAt", java.time.Instant.now())
                .append("expiresAt", java.time.Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 days
            
            configCollection.insertOne(configDoc);
            return configId;
            
        } catch (Exception e) {
            System.err.println("Failed to store configuration in database: " + e.getMessage());
            return null;
        }
    }
    
    private Document getConfigFromDatabase(String configId) {
        try {
            MongoCollection<Document> configCollection = Main.database.getCollection("shared_configs");
            
            Document query = new Document("_id", configId)
                .append("expiresAt", new Document("$gt", java.time.Instant.now()));
            
            return configCollection.find(query).first();
            
        } catch (Exception e) {
            System.err.println("Failed to retrieve configuration from database: " + e.getMessage());
            return null;
        }
    }
    
    private void parseAndApplyConfig(String userId, String configContent) {
        try {
            User user = Main.userService.getOrCreateUser(userId);
            String[] lines = configContent.split("\n");
            
            boolean inDictionarySection = false;
            boolean inSourceChancesSection = false;
            
            for (String line : lines) {
                line = line.trim();
                
                // Check for section headers
                if (line.contains("DICTIONARY ASSIGNMENTS")) {
                    inDictionarySection = true;
                    inSourceChancesSection = false;
                    continue;
                } else if (line.contains("SOURCE CHANCES CONFIGURATION")) {
                    inDictionarySection = false;
                    inSourceChancesSection = true;
                    continue;
                } else if (line.contains("YOUR DICTIONARIES") || line.contains("USAGE STATISTICS")) {
                    inDictionarySection = false;
                    inSourceChancesSection = false;
                    continue;
                }
                
                // Parse dictionary assignments
                if (inDictionarySection && line.contains(":") && !line.startsWith("-") && !line.startsWith("=")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String sourceName = parts[0].trim().toLowerCase();
                        String dictInfo = parts[1].trim();
                        
                        // Convert display names back to source keys
                        String sourceKey = switch (sourceName) {
                            case "tenor gifs" -> "tenor";
                            case "reddit" -> "reddit";
                            case "google images" -> "google";
                            default -> sourceName;
                        };
                        
                        // Extract dictionary ID from parentheses
                        if (dictInfo.contains("(") && dictInfo.contains(")")) {
                            int start = dictInfo.lastIndexOf("(") + 1;
                            int end = dictInfo.lastIndexOf(")");
                            if (start < end) {
                                String dictionaryId = dictInfo.substring(start, end);
                                
                                // Verify dictionary exists and user has access
                                Optional<Dictionary> dictOpt = dictionaryService.getDictionary(dictionaryId);
                                if (dictOpt.isPresent() && dictOpt.get().canBeViewedBy(userId)) {
                                    dictionaryService.assignDictionary(userId, sourceKey, dictionaryId);
                                }
                            }
                        }
                    }
                }
                
                // Parse source chances configuration
                if (inSourceChancesSection && line.contains("✅") || line.contains("❌")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        String sourceName = parts[0].toLowerCase();
                        boolean enabled = line.contains("✅");
                        
                        try {
                            // Extract chance percentage (last part should be the number)
                            String chanceStr = parts[parts.length - 1];
                            double chance = Double.parseDouble(chanceStr);
                            
                            // Convert display names back to source keys
                            String sourceKey = switch (sourceName) {
                                case "tenor" -> "tenor";
                                case "reddit" -> "reddit";
                                case "google" -> "google";
                                default -> sourceName;
                            };
                            
                            // Apply the image options
                            ImageOptions imageOptions = user.getImageOptions(sourceKey);
                            if (imageOptions == null) {
                                // Create new ImageOptions if it doesn't exist
                                imageOptions = new ImageOptions(sourceKey, enabled, chance);
                            } else {
                                // Update existing ImageOptions
                                imageOptions.setEnabled(enabled);
                                imageOptions.setChance(chance);
                            }
                            user.setChances(imageOptions);
                            
                        } catch (NumberFormatException e) {
                            // Skip invalid chance values
                        }
                    }
                }
            }
            
            // Save the updated user configuration
            Main.userService.updateUser(user);
            
        } catch (Exception e) {
            System.err.println("Failed to parse and apply configuration: " + e.getMessage());
        }
    }
    
    private String formatSourceName(String source) {
        return switch (source) {
            case "tenor" -> "Tenor GIFs";
            case "reddit" -> "Reddit";
            case "google" -> "Google Images";
            default -> source.substring(0, 1).toUpperCase() + source.substring(1);
        };
    }
    
    private void sendSuccess(SlashCommandInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("✅ Success")
            .setDescription(message)
            .setColor(SUCCESS_COLOR);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void sendError(SlashCommandInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("❌ Error")
            .setDescription(message)
            .setColor(ERROR_COLOR);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}