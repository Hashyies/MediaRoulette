package me.hash.mediaroulette.bot.commands.minigame;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.Transaction;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.model.minigame.MediaHuntGame;
import me.hash.mediaroulette.utils.GameManager;
import me.hash.mediaroulette.utils.QuestGenerator;
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

public class MediaHuntCommand extends ListenerAdapter implements CommandHandler {

    private static final Color GAME_COLOR = new Color(255, 20, 147); // Deep Pink
    private static final Color SUCCESS_COLOR = new Color(0, 255, 127); // Spring Green
    private static final Color WARNING_COLOR = new Color(255, 165, 0); // Orange

    @Override
    public CommandData getCommandData() {
        return Commands.slash("mediahunt", "🎮 Start or join a cooperative media hunting game")
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("mediahunt")) return;

        event.deferReply().queue();
        Bot.executor.execute(() -> {
            String userId = event.getUser().getId();
            String channelId = event.getChannel().getId();

            // Check if user is already in a game
            if (GameManager.getInstance().isPlayerInGame(userId)) {
                MediaHuntGame currentGame = GameManager.getInstance().getGameByPlayer(userId);
                showGameStatus(event, currentGame);
                return;
            }

            // Check if channel has an active game
            if (GameManager.getInstance().isChannelInGame(channelId)) {
                MediaHuntGame channelGame = GameManager.getInstance().getGameByChannel(channelId);
                showJoinGameOption(event, channelGame);
                return;
            }

            // Show game creation menu
            showGameCreationMenu(event);
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("hunt:")) return;

        event.deferEdit().queue();
        Bot.executor.execute(() -> {
            String[] parts = event.getComponentId().split(":");
            String action = parts[1];

            switch (action) {
                case "create" -> handleCreateGame(event, parts[2]);
                case "join" -> handleJoinGame(event);
                case "start" -> handleStartGame(event);
                case "leave" -> handleLeaveGame(event);
                case "submit" -> handleTargetSubmission(event, MediaHuntGame.TargetType.valueOf(parts[2]));
                case "end" -> handleEndGame(event);
                case "refresh" -> handleRefresh(event);
            }
        });
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("hunt:target")) return;

        event.deferEdit().queue();
        Bot.executor.execute(() -> {
            String targetType = event.getValues().get(0);
            handleTargetSubmission(event, MediaHuntGame.TargetType.valueOf(targetType));
        });
    }

    private void showGameCreationMenu(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎮 Media Hunt - Create Game")
                .setDescription("Choose a difficulty level to start a new cooperative media hunting game!")
                .setColor(GAME_COLOR)
                .addField("🟢 Easy", "3 targets • 2 minutes • 50-100 coins", true)
                .addField("🟡 Medium", "5 targets • 3 minutes • 100-200 coins", true)
                .addField("🔴 Hard", "7 targets • 4 minutes • 200-400 coins", true)
                .addField("⚫ Extreme", "10 targets • 5 minutes • 400-800 coins", true)
                .addField("📋 How to Play", "Work together to find different types of media using bot commands. Each player can contribute!", false)
                .setFooter("Up to 6 players can join • Rewards are shared based on contribution")
                .setTimestamp(Instant.now());

        List<Button> buttons = List.of(
                Button.success("hunt:create:EASY", "🟢 Easy"),
                Button.primary("hunt:create:MEDIUM", "🟡 Medium"),
                Button.secondary("hunt:create:HARD", "🔴 Hard"),
                Button.danger("hunt:create:EXTREME", "⚫ Extreme")
        );

        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(buttons))
                .queue();
    }

    private void showJoinGameOption(SlashCommandInteractionEvent event, MediaHuntGame game) {
        EmbedBuilder embed = createGameEmbed(game)
                .setTitle("🎮 Media Hunt - Join Game")
                .setDescription("There's an active game in this channel! Join the hunt!");

        List<Button> buttons = new ArrayList<>();
        if (game.getStatus() == MediaHuntGame.GameStatus.WAITING_FOR_PLAYERS) {
            buttons.add(Button.success("hunt:join", "🚀 Join Game"));
        }
        buttons.add(Button.secondary("hunt:refresh", "🔄 Refresh"));

        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(buttons))
                .queue();
    }

    private void handleCreateGame(ButtonInteractionEvent event, String difficulty) {
        String userId = event.getUser().getId();
        String channelId = event.getChannel().getId();

        MediaHuntGame game = GameManager.getInstance().createGame(userId, channelId, 
                MediaHuntGame.GameDifficulty.valueOf(difficulty));

        if (game == null) {
            updateWithError(event, "❌ Failed to create game. You might already be in a game or channel is busy.");
            return;
        }

        showGameLobby(event, game);
    }

    private void handleJoinGame(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        String channelId = event.getChannel().getId();

        boolean joined = GameManager.getInstance().joinGameByChannel(channelId, userId);
        if (!joined) {
            updateWithError(event, "❌ Failed to join game. Game might be full or you're already in another game.");
            return;
        }

        MediaHuntGame game = GameManager.getInstance().getGameByChannel(channelId);
        showGameLobby(event, game);
    }

    private void showGameLobby(ButtonInteractionEvent event, MediaHuntGame game) {
        EmbedBuilder embed = createGameEmbed(game)
                .setTitle("🎮 Media Hunt - Lobby")
                .setDescription("Game created! Waiting for players to join...");

        List<Button> buttons = new ArrayList<>();
        if (game.getHostUserId().equals(event.getUser().getId())) {
            buttons.add(Button.success("hunt:start", "▶️ Start Game"));
        }
        buttons.add(Button.danger("hunt:leave", "🚪 Leave"));
        buttons.add(Button.secondary("hunt:refresh", "🔄 Refresh"));

        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(buttons))
                .queue();
    }

    private void handleStartGame(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        MediaHuntGame game = GameManager.getInstance().getGameByPlayer(userId);

        if (game == null || !game.getHostUserId().equals(userId)) {
            updateWithError(event, "❌ Only the host can start the game.");
            return;
        }

        GameManager.getInstance().startGame(game.getGameId());
        showActiveGame(event, game);
    }

    private void showActiveGame(ButtonInteractionEvent event, MediaHuntGame game) {
        EmbedBuilder embed = createGameEmbed(game)
                .setTitle("🎮 Media Hunt - Active Game")
                .setDescription("🚀 **Game Started!** Find the targets by using bot commands!");

        // Add target selection menu
        StringSelectMenu.Builder targetMenu = StringSelectMenu.create("hunt:target")
                .setPlaceholder("🎯 Found a target? Select it here...");

        for (MediaHuntGame.TargetType target : game.getRemainingTargets()) {
            targetMenu.addOption(target.getEmoji() + " " + target.getDescription(), 
                    target.name(), "Use /" + target.getSourceHint() + " commands");
        }

        List<Button> buttons = List.of(
                Button.secondary("hunt:refresh", "🔄 Refresh"),
                Button.danger("hunt:end", "🛑 End Game")
        );

        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(targetMenu.build()), ActionRow.of(buttons))
                .queue();
    }

    private void showActiveGame(StringSelectInteractionEvent event, MediaHuntGame game) {
        EmbedBuilder embed = createGameEmbed(game)
                .setTitle("🎮 Media Hunt - Active Game")
                .setDescription("🚀 **Game Started!** Find the targets by using bot commands!");

        // Add target selection menu
        StringSelectMenu.Builder targetMenu = StringSelectMenu.create("hunt:target")
                .setPlaceholder("🎯 Found a target? Select it here...");

        for (MediaHuntGame.TargetType target : game.getRemainingTargets()) {
            targetMenu.addOption(target.getEmoji() + " " + target.getDescription(), 
                    target.name(), "Use /" + target.getSourceHint() + " commands");
        }

        List<Button> buttons = List.of(
                Button.secondary("hunt:refresh", "🔄 Refresh"),
                Button.danger("hunt:end", "🛑 End Game")
        );

        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(targetMenu.build()), ActionRow.of(buttons))
                .queue();
    }

    private void handleTargetSubmission(StringSelectInteractionEvent event, MediaHuntGame.TargetType targetType) {
        String userId = event.getUser().getId();
        boolean success = GameManager.getInstance().submitTarget(userId, targetType, "Found via command");

        if (!success) {
            updateWithError(event, "❌ Target already found or invalid submission.");
            return;
        }

        MediaHuntGame game = GameManager.getInstance().getGameByPlayer(userId);
        
        // Update quest progress
        User user = Main.userService.getOrCreateUser(userId);
        QuestGenerator.updateQuestProgress(user, me.hash.mediaroulette.model.Quest.QuestType.SOCIAL_INTERACTION);
        Main.userService.updateUser(user);

        if (game.getStatus() == MediaHuntGame.GameStatus.COMPLETED) {
            handleGameCompletion(event, game);
        } else {
            showActiveGame(event, game);
        }
    }

    private void handleTargetSubmission(ButtonInteractionEvent event, MediaHuntGame.TargetType targetType) {
        String userId = event.getUser().getId();
        boolean success = GameManager.getInstance().submitTarget(userId, targetType, "Found via command");

        if (!success) {
            updateWithError(event, "❌ Target already found or invalid submission.");
            return;
        }

        MediaHuntGame game = GameManager.getInstance().getGameByPlayer(userId);
        
        // Update quest progress
        User user = Main.userService.getOrCreateUser(userId);
        QuestGenerator.updateQuestProgress(user, me.hash.mediaroulette.model.Quest.QuestType.SOCIAL_INTERACTION);
        Main.userService.updateUser(user);

        if (game.getStatus() == MediaHuntGame.GameStatus.COMPLETED) {
            handleGameCompletion(event, game);
        } else {
            showActiveGame(event, game);
        }
    }

    private void handleGameCompletion(StringSelectInteractionEvent event, MediaHuntGame game) {
        Map<String, Long> rewards = game.calculateRewards();
        
        // Distribute rewards
        for (Map.Entry<String, Long> entry : rewards.entrySet()) {
            User user = Main.userService.getOrCreateUser(entry.getKey());
            user.addCoins(entry.getValue(), Transaction.TransactionType.QUEST_REWARD, 
                    "Media Hunt completion reward");
            Main.userService.updateUser(user);
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎉 Media Hunt - Completed!")
                .setDescription("Congratulations! You found all targets!")
                .setColor(SUCCESS_COLOR)
                .addField("🏆 Results", game.getGameSummary(), false)
                .addField("💰 Rewards Distributed", formatRewards(rewards), false)
                .setTimestamp(Instant.now());

        event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
        
        // Clean up game
        GameManager.getInstance().removeGame(game.getGameId());
    }

    private void handleGameCompletion(ButtonInteractionEvent event, MediaHuntGame game) {
        Map<String, Long> rewards = game.calculateRewards();
        
        // Distribute rewards
        for (Map.Entry<String, Long> entry : rewards.entrySet()) {
            User user = Main.userService.getOrCreateUser(entry.getKey());
            user.addCoins(entry.getValue(), Transaction.TransactionType.QUEST_REWARD, 
                    "Media Hunt completion reward");
            Main.userService.updateUser(user);
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎉 Media Hunt - Completed!")
                .setDescription("Congratulations! You found all targets!")
                .setColor(SUCCESS_COLOR)
                .addField("🏆 Results", game.getGameSummary(), false)
                .addField("💰 Rewards Distributed", formatRewards(rewards), false)
                .setTimestamp(Instant.now());

        event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
        
        // Clean up game
        GameManager.getInstance().removeGame(game.getGameId());
    }

    private EmbedBuilder createGameEmbed(MediaHuntGame game) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(GAME_COLOR)
                .setTimestamp(Instant.now());

        // Game info
        embed.addField("🎯 Difficulty", game.getDifficulty().name(), true);
        embed.addField("👥 Players", game.getPlayerIds().size() + "/" + game.getMaxPlayers(), true);
        embed.addField("⏱️ Duration", game.getDifficulty().getDurationSeconds() / 60 + " minutes", true);

        // Progress
        if (game.getStatus() == MediaHuntGame.GameStatus.IN_PROGRESS) {
            embed.addField("📊 Progress", 
                    game.getProgressBar() + "\n" + 
                    game.getCompletedTargets().size() + "/" + game.getTargets().size() + " targets found", false);
            embed.addField("⏰ Time Left", game.getTimeRemainingSeconds() / 60 + ":" + 
                    String.format("%02d", game.getTimeRemainingSeconds() % 60), true);
        }

        // Player list
        StringBuilder players = new StringBuilder();
        for (String playerId : game.getPlayerIds()) {
            players.append("<@").append(playerId).append("> ");
            if (playerId.equals(game.getHostUserId())) players.append("👑 ");
        }
        embed.addField("👥 Players", players.toString(), false);

        return embed;
    }

    private String formatRewards(Map<String, Long> rewards) {
        StringBuilder sb = new StringBuilder();
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        for (Map.Entry<String, Long> entry : rewards.entrySet()) {
            sb.append("<@").append(entry.getKey()).append(">: ")
              .append(formatter.format(entry.getValue())).append(" coins\n");
        }
        return sb.toString();
    }

    private void handleLeaveGame(ButtonInteractionEvent event) {
        GameManager.getInstance().leaveGame(event.getUser().getId());
        updateWithMessage(event, "✅ Left the game successfully.");
    }

    private void handleRefresh(ButtonInteractionEvent event) {
        MediaHuntGame game = GameManager.getInstance().getGameByPlayer(event.getUser().getId());
        if (game != null) {
            showGameStatus(event, game);
        }
    }

    private void showGameStatus(SlashCommandInteractionEvent event, MediaHuntGame game) {
        EmbedBuilder embed = createGameEmbed(game);
        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showGameStatus(ButtonInteractionEvent event, MediaHuntGame game) {
        EmbedBuilder embed = createGameEmbed(game);
        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void updateWithError(ButtonInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Error")
                .setDescription(message)
                .setColor(Color.RED);
        event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
    }

    private void updateWithError(StringSelectInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Error")
                .setDescription(message)
                .setColor(Color.RED);
        event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
    }

    private void updateWithMessage(ButtonInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
                .setDescription(message)
                .setColor(SUCCESS_COLOR);
        event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
    }

    private void handleEndGame(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        MediaHuntGame game = GameManager.getInstance().getGameByPlayer(userId);
        
        if (game != null && game.getHostUserId().equals(userId)) {
            GameManager.getInstance().endGame(game.getGameId());
            updateWithMessage(event, "🛑 Game ended by host.");
        }
    }
}