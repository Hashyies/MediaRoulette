package me.hash.mediaroulette.bot.commands.images;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Emoji;
import me.hash.mediaroulette.bot.MediaContainerManager;
import me.hash.mediaroulette.bot.errorHandler;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.utils.Locale;
import me.hash.mediaroulette.utils.MaintenanceChecker;
import me.hash.mediaroulette.utils.QuestGenerator;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.replacer.ComponentReplacer;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class getRandomImage extends ListenerAdapter implements CommandHandler {

    @Override
    public CommandData getCommandData() {
        return Commands.slash("random", "Sends a random image")
                .addSubcommands(
                        new SubcommandData("all", "Sends images from all sources")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("picsum", "Sends a random image from picsum")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("imgur", "Sends a random image from imgur")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("rulee34xxx", "Sends a random image from rulee34 website")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("google", "Sends a random image from google")
                                .addOption(OptionType.STRING, "query", "What image should be searched for?", false, true)
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("reddit", "Sends a random image from reddit")
                                .addOption(OptionType.STRING, "query", "Which subreddit should the image be retrieved from?", false, true)
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("tenor", "Sends a random gif from tenor")
                                .addOption(OptionType.STRING, "query", "What gif should be searched for?", false, true)
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the gif keep generating?"),
                        new SubcommandData("4chan", "Sends a random image from 4chan")
                                .addOption(OptionType.STRING, "query", "Which board to retrieve image from?", false, true)
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("movie", "Sends a random movie from TMDB")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("tvshow", "Sends a random TV Show from TMDB")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("youtube", "Sends a random YouTube video")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("short", "Sends a random Short from TMDB")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("urban", "Sends a random word from The Urban Dictionary")
                                .addOption(OptionType.STRING, "query", "What word should be defined?")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the word keep generating?")
                ).setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    // Concurrent map to store active message data related to interactions
    private static final Map<Long, MessageData> ACTIVE_MESSAGES = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService CLEANUP_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final long INACTIVITY_TIMEOUT = TimeUnit.MINUTES.toMillis(3); // 3 minutes timeout

    static {
        CLEANUP_EXECUTOR.scheduleAtFixedRate(() -> {
            long now = Instant.now().toEpochMilli();
            ACTIVE_MESSAGES.entrySet().removeIf(entry -> {
                MessageData data = entry.getValue();
                if (now - data.getLastInteractionTime() > INACTIVITY_TIMEOUT) {
                    data.disableButtons();
                    return true;
                }
                return false;
            });
        }, 1, 1, TimeUnit.MINUTES);
    }

    private static final Map<Long, Long> COOLDOWNS = new ConcurrentHashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("random")) return;
        
        // Check maintenance mode first
        if (MaintenanceChecker.isMaintenanceBlocked(event)) {
            MaintenanceChecker.sendMaintenanceMessage(event);
            return;
        }
        
        // Track command usage and update user activity
        String userId = event.getUser().getId();
        Main.userService.trackCommandUsage(userId, "random");
        
        // Track query usage based on subcommand
        String subcommand = event.getSubcommandName();
        if (event.getOption("query") != null) {
            String query = event.getOption("query").getAsString();
            
            switch (subcommand) {
                case "reddit" -> {
                    Main.userService.addCustomSubreddit(userId, query);
                    Main.userService.trackSourceUsage(userId, "reddit");
                }
                case "google" -> {
                    Main.userService.addCustomQuery(userId, "google", query);
                    Main.userService.trackSourceUsage(userId, "google");
                }
                case "tenor" -> {
                    Main.userService.addCustomQuery(userId, "tenor", query);
                    Main.userService.trackSourceUsage(userId, "tenor");
                }
                case "4chan" -> {
                    Main.userService.addCustomQuery(userId, "4chan", query);
                    Main.userService.trackSourceUsage(userId, "4chan");
                }
            }
        } else {
            // Track source usage for commands without queries
            switch (subcommand) {
                case "all" -> Main.userService.trackSourceUsage(userId, "all");
                case "picsum" -> Main.userService.trackSourceUsage(userId, "picsum");
                case "imgur" -> Main.userService.trackSourceUsage(userId, "imgur");
                case "rulee34xxx" -> Main.userService.trackSourceUsage(userId, "rule34");
                case "movie" -> Main.userService.trackSourceUsage(userId, "tmdb-movie");
                case "tvshow" -> Main.userService.trackSourceUsage(userId, "tmdb-tv");
                case "youtube" -> Main.userService.trackSourceUsage(userId, "youtube");
                case "short" -> Main.userService.trackSourceUsage(userId, "youtube-shorts");
                case "urban" -> Main.userService.trackSourceUsage(userId, "urban-dictionary");
            }
        }
        
        User user = Main.userService.getOrCreateUser(userId);

        // Send loading container immediately using Components V2
        net.dv8tion.jda.api.components.container.Container loadingContainer = net.dv8tion.jda.api.components.container.Container.of(
                net.dv8tion.jda.api.components.section.Section.of(
                        net.dv8tion.jda.api.components.thumbnail.Thumbnail.fromUrl(event.getUser().getEffectiveAvatarUrl()),
                        net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("## <a:loading:1350829863157891094> Generating Image..."),
                        net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("**Please wait while we fetch your random image...**"),
                        net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("*This may take a few seconds*")
                )
        ).withAccentColor(new Color(88, 101, 242));
        
        event.replyComponents(loadingContainer).useComponentsV2().queue(interactionHook -> {
            Bot.executor.execute(() -> {
                try {
                    boolean shouldContinue = event.getOption("shouldcontinue") != null
                            && event.getOption("shouldcontinue").getAsBoolean();

                    ImageSource.fromName(subcommand.toUpperCase()).ifPresentOrElse(source -> {
                        try {
                            String query = event.getOption("query") == null ? null : event.getOption("query").getAsString();
                            Map<String, String> image = source.handle(event, query);

                            if (image == null || image.get("image") == null) {
                                errorHandler.sendErrorEmbed(event, new Locale(user.getLocale()).get("error.no_images_title"), new Locale(user.getLocale()).get("error.no_images_description"));
                                return;
                            }

                            // Track image generation in stats service
                            if (Main.statsService != null) {
                                Main.statsService.trackImageGenerated(userId, subcommand, user.isNsfw(), user.isPremium());
                                Main.statsService.trackCommandUsed(userId, "random", user.isPremium());
                                Main.statsService.trackUserActivity(userId, user.isPremium());
                            }

                            // Edit the loading message with the actual image using Components V2
                            MediaContainerManager.editLoadingToImageContainer(interactionHook, image, shouldContinue)
                                    .thenAccept(messageSent -> {
                                        MessageData data = new MessageData(
                                                messageSent.getIdLong(),
                                                subcommand,
                                                query,
                                                shouldContinue,
                                                event.getUser().getIdLong(),
                                                event.getChannel().getIdLong()
                                        );
                                        ACTIVE_MESSAGES.put(messageSent.getIdLong(), data);
                                        
                                        // Update quest progress for image generation
                                        QuestGenerator.onImageGenerated(user, subcommand);
                                        Main.userService.updateUser(user);
                                    })
                                    .exceptionally(ex -> {
                                        errorHandler.handleException(event, new Locale(user.getLocale()).get("error.unexpected_error"), new Locale(user.getLocale()).get("error.failed_to_send_image"), ex);
                                        return null;
                                    });

                        } catch (Exception e) {
                            errorHandler.handleException(event, new Locale(user.getLocale()).get("error.source_error_title"), new Locale(user.getLocale()).get("error.source_error_description"), e);
                        }
                    }, () -> errorHandler.sendErrorEmbed(event, new Locale(user.getLocale()).get("error.unknown_subcommand_title"), new Locale(user.getLocale()).get("error.unknown_subcommand_description")));
                } catch (Exception e) {
                    errorHandler.handleException(event, new Locale(user.getLocale()).get("error.unexpected_error"), e.getMessage(), e);
                }
                user.incrementImagesGenerated();
                Main.userService.updateUser(user);
            });
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        long messageId = event.getMessageIdLong();

        if (!ACTIVE_MESSAGES.containsKey(messageId)) {
            return;
        }
        
        // Check maintenance mode for button interactions too
        if (MaintenanceChecker.isMaintenanceBlocked(event)) {
            MaintenanceChecker.sendMaintenanceMessage(event);
            return;
        }

        event.deferEdit().queue();
        Bot.executor.execute(() -> {
            MessageData data = ACTIVE_MESSAGES.get(messageId);

            User user = Main.userService.getOrCreateUser(event.getUser().getId());


            data.updateLastInteractionTime();

            if (!data.isUserAllowed(event.getUser().getIdLong())) {
                // Show error container using hook since interaction is already acknowledged
                showErrorContainer(event, new Locale(user.getLocale()).get("error.unknown_button_title"), 
                                 new Locale(user.getLocale()).get("error.unknown_button_description"));
                return;
            }

            String buttonId = event.getButton().getId();

            switch (buttonId) {
                case "nsfw:continue":
                case "safe:continue":
                    handleContinue(event, data);
                    break;
                case "favorite":
                    handleFavorite(event);
                    break;
                case "nsfw":
                case "safe":
                    // Update quest progress for rating images
                    QuestGenerator.onImageRated(user);
                    Main.userService.updateUser(user);
                    disableAllButtonsInContainer(event);
                    break;
                case "exit":
                    disableAllButtonsInContainer(event);
                    break;
                case null:
                default:
                    // Show error container using hook since interaction is already acknowledged
                    showErrorContainer(event, new Locale(user.getLocale()).get("error.unknown_button_title"), 
                                     new Locale(user.getLocale()).get("error.unknown_button_description"));
            }
        });
    }

    private void handleContinue(ButtonInteractionEvent event, MessageData data) {
        User user = Main.userService.getOrCreateUser(event.getUser().getId());
        
        // Show loading container immediately using Components V2
        net.dv8tion.jda.api.components.container.Container loadingContainer = net.dv8tion.jda.api.components.container.Container.of(
                net.dv8tion.jda.api.components.section.Section.of(
                        net.dv8tion.jda.api.components.thumbnail.Thumbnail.fromUrl(event.getUser().getEffectiveAvatarUrl()),
                        net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("## " + Emoji.LOADING + " Generating Next Image..."),
                        net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("**Please wait while we fetch your next random image...**"),
                        net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("*This may take a few seconds*")
                )
        ).withAccentColor(new Color(88, 101, 242));

        // Edit to loading state first using Components V2
        event.getHook().editOriginalComponents(loadingContainer)
                .useComponentsV2()
                .queue(success -> {
                    // Now fetch the new image
                    ImageSource.fromName(data.getSubcommand().toUpperCase()).ifPresentOrElse(source -> {
                        try {
                            Map<String, String> image = source.handle(event, data.getQuery());
                            if (image == null || image.get("image") == null) {
                                // Show error container using hook since interaction is already acknowledged
                                showErrorContainer(event, new Locale(user.getLocale()).get("error.no_more_images_title"), 
                                                 new Locale(user.getLocale()).get("error.no_more_images_description"));
                                return;
                            }
                            // Use LoadingEmbeds to edit the loading message to the final image
                            MediaContainerManager.editLoadingToImageContainerFromHook(event.getHook(), image, true);
                        } catch (Exception e) {
                            // Show error container using hook since interaction is already acknowledged
                            showErrorContainer(event, new Locale(user.getLocale()).get("error.title"), e.getMessage());
                        }
                    }, () -> {
                        // Show error container using hook since interaction is already acknowledged
                        showErrorContainer(event, new Locale(user.getLocale()).get("error.title"), 
                                         new Locale(user.getLocale()).get("error.invalid_subcommand_description"));
                    });
                    user.incrementImagesGenerated();
                    
                    // Update quest progress for continue button
                    QuestGenerator.onImageGenerated(user, data.getSubcommand());
                    Main.userService.updateUser(user);
                });
    }

    private void handleFavorite(ButtonInteractionEvent event) {
        User user = Main.userService.getOrCreateUser(event.getUser().getId());
        try {
            // For container messages, we need to extract the favorite info differently
            // Since containers don't use embeds, we'll get the info from the message data
            MessageData data = ACTIVE_MESSAGES.get(event.getMessageIdLong());
            if (data == null) {
                showErrorContainer(event, new Locale(user.getLocale()).get("error.no_image_title"), 
                                 new Locale(user.getLocale()).get("error.no_image_description"));
                return;
            }

            // For now, we'll use the subcommand as description and get image URL from the container
            String description = "Random " + data.getSubcommand() + " image";
            String imageUrl = extractImageUrlFromContainer(event.getMessage());

            // Add a favorite using the new favorites model ("image" type)
            user.addFavorite(description, imageUrl, "image");

            // Update quest progress for favoriting
            QuestGenerator.onImageFavorited(user);

            // Persist the update via the service
            Main.userService.updateUser(user);

            // Disable the favorite button once it's been handled
            disableButtonInContainer(event, "favorite");
        } catch (Exception e) {
            // Show error container using hook since interaction is already acknowledged
            showErrorContainer(event, new Locale(user.getLocale()).get("error.title"), e.getMessage());
        }
    }

    private static class MessageData {
        private final long messageId;
        private final String subcommand, query;
        private final boolean shouldContinue;
        private final long userId;
        private final long channelId;
        private long lastInteractionTime;

        public MessageData(long messageId, String subcommand, String query, boolean shouldContinue, long userId, long channelId) {
            this.messageId = messageId;
            this.subcommand = subcommand;
            this.query = query;
            this.shouldContinue = shouldContinue;
            this.userId = userId;
            this.channelId = channelId;
            this.lastInteractionTime = Instant.now().toEpochMilli();
        }

        public String getQuery() {
            return query;
        }

        public String getSubcommand() {
            return subcommand;
        }

        public boolean isShouldContinue() {
            return shouldContinue;
        }

        public boolean isUserAllowed(long userId) {
            return this.userId == userId;
        }

        public long getLastInteractionTime() {
            return lastInteractionTime;
        }

        public void updateLastInteractionTime() {
            this.lastInteractionTime = Instant.now().toEpochMilli();
        }

        public void disableButtons() {
            Bot.getShardManager().getTextChannelById(channelId).retrieveMessageById(messageId).queue(message -> {
                if (message.getActionRows().isEmpty()) return;

                MessageComponentTree components = message.getComponentTree();
                
                // Use ComponentReplacer to disable all buttons
                ComponentReplacer replacer = ComponentReplacer.of(
                    Button.class,
                    button -> true, // Match all buttons
                        Button::asDisabled
                );
                
                MessageComponentTree updated = components.replace(replacer);

                message.editMessageComponents(updated)
                        .queue(success -> System.out.println("Buttons disabled for message " + messageId),
                               error -> System.out.println("Could not disable buttons for message " + messageId));
            }, throwable -> {
                System.out.println("Message " + messageId + " in channel " + channelId + " does not exist anymore.");
            });
        }
    }

    /**
     * Disables a specific button in a container message
     */
    private static void disableButtonInContainer(ButtonInteractionEvent event, String buttonId) {
        updateButtonInContainer(event, buttonId);
    }

    /**
     * Disables all buttons in a container message
     */
    private static void disableAllButtonsInContainer(ButtonInteractionEvent event) {
        updateAllButtonsInContainer(event);
    }

    private static void updateButtonInContainer(ButtonInteractionEvent event, String buttonId) {
        try {
            MessageComponentTree components = event.getMessage().getComponentTree();
            ComponentReplacer replacer = ComponentReplacer.of(
                    Button.class,
                    button -> buttonId.equals(button.getId()),
                    Button::asDisabled
            );
            MessageComponentTree updated = components.replace(replacer);
            
            // Since we already deferred the interaction, use the hook
            event.getHook().editOriginalComponents(updated)
                    .useComponentsV2()
                    .queue(null, error -> System.err.println("Failed to disable button: " + error.getMessage()));
        } catch (Exception e) {
            System.err.println("Error updating button in container: " + e.getMessage());
        }
    }

    private static void updateAllButtonsInContainer(ButtonInteractionEvent event) {
        try {
            MessageComponentTree components = event.getMessage().getComponentTree();
            ComponentReplacer replacer = ComponentReplacer.of(
                    Button.class,
                    button -> true,
                    Button::asDisabled
            );
            MessageComponentTree updated = components.replace(replacer);
            
            // Since we already deferred the interaction, use the hook
            event.getHook().editOriginalComponents(updated)
                    .useComponentsV2()
                    .queue(null, error -> System.err.println("Failed to disable all buttons: " + error.getMessage()));
        } catch (Exception e) {
            System.err.println("Error updating all buttons in container: " + e.getMessage());
        }
    }

    /**
     * Shows an error container message
     */
    private static void showErrorContainer(ButtonInteractionEvent event, String title, String description) {
        try {
            net.dv8tion.jda.api.components.container.Container errorContainer = net.dv8tion.jda.api.components.container.Container.of(
                    net.dv8tion.jda.api.components.section.Section.of(
                            net.dv8tion.jda.api.components.thumbnail.Thumbnail.fromUrl(event.getUser().getEffectiveAvatarUrl()),
                            net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("## ❌ " + title),
                            net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("**" + description + "**"),
                            net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("*Please try again*")
                    )
            ).withAccentColor(Color.RED);

            event.getHook().editOriginalComponents(errorContainer)
                    .useComponentsV2()
                    .queue(null, error -> System.err.println("Failed to show error container: " + error.getMessage()));
        } catch (Exception e) {
            System.err.println("Error creating error container: " + e.getMessage());
        }
    }

    /**
     * Extracts image URL from a container message (simplified approach)
     */
    private static String extractImageUrlFromContainer(net.dv8tion.jda.api.entities.Message message) {
        // For now, return null as extracting from containers is complex
        // In a real implementation, you'd need to parse the container structure
        // or store the image URL in the MessageData for easier retrieval
        return null;
    }
}