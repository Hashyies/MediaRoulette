package me.hash.mediaroulette.bot.commands.images;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.bot.errorHandler;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.utils.user.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

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
                                .addOption(OptionType.STRING, "query", "What image should be searched for?")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("reddit", "Sends a random image from reddit")
                                .addOption(OptionType.STRING, "query", "Which subreddit should the image be retrieved from?")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the image keep generating?"),
                        new SubcommandData("tenor", "Sends a random gif from tenor")
                                .addOption(OptionType.STRING, "query", "What gif should be searched for?")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                        "Should the gif keep generating?"),
                        new SubcommandData("4chan", "Sends a random image from 4chan")
                                .addOption(OptionType.STRING, "query", "Which board to retrieve image from?")
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

        event.deferReply().queue(interactionHook -> {
            Bot.executor.execute(() -> {
                try {
                    String subcommand = event.getSubcommandName();
                    boolean shouldContinue = event.getOption("shouldcontinue") != null
                            && event.getOption("shouldcontinue").getAsBoolean();

                    ImageSource.fromName(subcommand.toUpperCase()).ifPresentOrElse(source -> {
                        try {
                            String query = event.getOption("query") == null ? null : event.getOption("query").getAsString();
                            Map<String, String> image = source.handle(event, query);

                            if (image == null || image.get("image") == null) {
                                errorHandler.sendErrorEmbed(event, "No Images Found", "This source appears to be empty.");
                                return;
                            }

                            Embeds.sendImageEmbed(interactionHook.getInteraction(), image, shouldContinue)
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
                                    })
                                    .exceptionally(ex -> {
                                        errorHandler.handleException(event, "Unexpected Error", "Failed to send the image embed.", ex);
                                        return null;
                                    });

                        } catch (Exception e) {
                            errorHandler.handleException(event, "Source Error", "Failed to retrieve images.", e);
                        }
                    }, () -> errorHandler.sendErrorEmbed(event, "Unknown Subcommand", "This subcommand is not recognized."));
                } catch (Exception e) {
                    errorHandler.handleException(event, "Unexpected Error", e.getMessage(), e);
                }
            });
        });
    }


    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        long messageId = event.getMessageIdLong();

        if (!ACTIVE_MESSAGES.containsKey(messageId)) {
            return;
        }

        event.deferEdit().queue();
        Bot.executor.execute(() -> {
            MessageData data = ACTIVE_MESSAGES.get(messageId);

            data.updateLastInteractionTime();

            if (!data.isUserAllowed(event.getUser().getIdLong())) {
                errorHandler.sendErrorEmbed(event, "Not Allowed", "Only the original user can interact with this button.");
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
                case "exit":
                    Embeds.disableAllButtons(event);
                    break;
                case null:
                default:
                    errorHandler.sendErrorEmbed(event, "Unknown Button", "This button ID is not recognized.");
            }
        });
    }

    private void handleContinue(ButtonInteractionEvent event, MessageData data) {
        ImageSource.fromName(data.getSubcommand().toUpperCase()).ifPresentOrElse(source -> {
            try {
                Map<String, String> image = source.handle(event, data.getQuery());
                if (image == null || image.get("image") == null) {
                    errorHandler.sendErrorEmbed(event, "No More Images", "No more images are available for this source.");
                    return;
                }
                Embeds.editImageEmbed(event, image);
            } catch (Exception e) {
                errorHandler.handleException(event, "Error", e.getMessage(), e);
            }
        }, () -> errorHandler.sendErrorEmbed(event, "Error", "Invalid subcommand data."));
    }

    private void handleFavorite(ButtonInteractionEvent event) {
        try {
            User user = User.get(Main.database, event.getUser().getId());
            if (event.getMessage().getEmbeds().isEmpty()) {
                errorHandler.sendErrorEmbed(event, "No Image Found", "This message does not contain a valid embed.");
                return;
            }

            user.addFavorite(
                    event.getMessage().getEmbeds().get(0).getDescription(),
                    event.getMessage().getEmbeds().get(0).getImage().getUrl(),
                    "image"
            );

            Embeds.disableButton(event, "favorite");
        } catch (Exception e) {
            errorHandler.handleException(event, "Error", "Failed to add this image to favorites.", e);
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

                message.editMessageComponents(
                        message.getActionRows().stream()
                                .map(row -> row.withDisabled(true))
                                .toArray(net.dv8tion.jda.api.interactions.components.LayoutComponent[]::new)
                ).queue(success -> System.out.println("Buttons disabled for message " + messageId),
                        error -> System.out.println("Could not disable buttons for message " + messageId));
            }, throwable -> {
                System.out.println("Message " + messageId + " in channel " + channelId + " does not exist anymore.");
            });
        }
    }
}
