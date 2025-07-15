package me.hash.mediaroulette.utils;

import me.hash.mediaroulette.model.minigame.MediaHuntGame;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameManager {
    private static GameManager instance;
    private final Map<String, MediaHuntGame> activeGames; // gameId -> game
    private final Map<String, String> channelGames; // channelId -> gameId
    private final Map<String, String> playerGames; // userId -> gameId
    private final ScheduledExecutorService scheduler;

    private GameManager() {
        this.activeGames = new ConcurrentHashMap<>();
        this.channelGames = new ConcurrentHashMap<>();
        this.playerGames = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Start cleanup task
        startCleanupTask();
    }

    public static GameManager getInstance() {
        if (instance == null) {
            synchronized (GameManager.class) {
                if (instance == null) {
                    instance = new GameManager();
                }
            }
        }
        return instance;
    }

    public MediaHuntGame createGame(String hostUserId, String channelId, MediaHuntGame.GameDifficulty difficulty) {
        // Check if channel already has an active game
        if (channelGames.containsKey(channelId)) {
            return null;
        }

        // Check if user is already in a game
        if (playerGames.containsKey(hostUserId)) {
            return null;
        }

        MediaHuntGame game = new MediaHuntGame(hostUserId, channelId, difficulty);
        activeGames.put(game.getGameId(), game);
        channelGames.put(channelId, game.getGameId());
        playerGames.put(hostUserId, game.getGameId());

        return game;
    }

    public boolean joinGame(String gameId, String userId) {
        MediaHuntGame game = activeGames.get(gameId);
        if (game == null) return false;

        // Check if user is already in a game
        if (playerGames.containsKey(userId)) {
            return false;
        }

        boolean joined = game.addPlayer(userId);
        if (joined) {
            playerGames.put(userId, gameId);
        }
        return joined;
    }

    public boolean joinGameByChannel(String channelId, String userId) {
        String gameId = channelGames.get(channelId);
        if (gameId == null) return false;
        return joinGame(gameId, userId);
    }

    public boolean leaveGame(String userId) {
        String gameId = playerGames.get(userId);
        if (gameId == null) return false;

        MediaHuntGame game = activeGames.get(gameId);
        if (game == null) return false;

        boolean left = game.removePlayer(userId);
        if (left) {
            playerGames.remove(userId);
            
            // If no players left, remove the game
            if (game.getPlayerIds().isEmpty()) {
                removeGame(gameId);
            }
        }
        return left;
    }

    public boolean startGame(String gameId) {
        MediaHuntGame game = activeGames.get(gameId);
        if (game == null) return false;

        game.startGame();
        
        // Schedule automatic game end
        scheduler.schedule(() -> {
            MediaHuntGame currentGame = activeGames.get(gameId);
            if (currentGame != null && currentGame.isExpired()) {
                currentGame.setStatus(MediaHuntGame.GameStatus.EXPIRED);
            }
        }, game.getDifficulty().getDurationSeconds(), TimeUnit.SECONDS);

        return true;
    }

    public boolean submitTarget(String userId, MediaHuntGame.TargetType targetType, String evidence) {
        String gameId = playerGames.get(userId);
        if (gameId == null) return false;

        MediaHuntGame game = activeGames.get(gameId);
        if (game == null) return false;

        return game.submitTarget(userId, targetType, evidence);
    }

    public MediaHuntGame getGameByPlayer(String userId) {
        String gameId = playerGames.get(userId);
        if (gameId == null) return null;
        return activeGames.get(gameId);
    }

    public MediaHuntGame getGameByChannel(String channelId) {
        String gameId = channelGames.get(channelId);
        if (gameId == null) return null;
        return activeGames.get(gameId);
    }

    public MediaHuntGame getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public List<MediaHuntGame> getAllActiveGames() {
        return new ArrayList<>(activeGames.values());
    }

    public List<MediaHuntGame> getWaitingGames() {
        return activeGames.values().stream()
                .filter(game -> game.getStatus() == MediaHuntGame.GameStatus.WAITING_FOR_PLAYERS)
                .toList();
    }

    public List<MediaHuntGame> getInProgressGames() {
        return activeGames.values().stream()
                .filter(game -> game.getStatus() == MediaHuntGame.GameStatus.IN_PROGRESS)
                .toList();
    }

    public boolean endGame(String gameId) {
        MediaHuntGame game = activeGames.get(gameId);
        if (game == null) return false;

        // Set status based on completion
        if (game.getCompletedTargets().size() >= game.getTargets().size()) {
            game.setStatus(MediaHuntGame.GameStatus.COMPLETED);
        } else {
            game.setStatus(MediaHuntGame.GameStatus.FAILED);
        }

        return true;
    }

    public void removeGame(String gameId) {
        MediaHuntGame game = activeGames.remove(gameId);
        if (game != null) {
            // Clean up mappings
            channelGames.remove(game.getChannelId());
            for (String playerId : game.getPlayerIds()) {
                playerGames.remove(playerId);
            }
        }
    }

    public boolean isPlayerInGame(String userId) {
        return playerGames.containsKey(userId);
    }

    public boolean isChannelInGame(String channelId) {
        return channelGames.containsKey(channelId);
    }

    public int getActiveGameCount() {
        return activeGames.size();
    }

    public int getTotalPlayersInGames() {
        return activeGames.values().stream()
                .mapToInt(game -> game.getPlayerIds().size())
                .sum();
    }

    private void startCleanupTask() {
        // Run cleanup every 5 minutes
        scheduler.scheduleAtFixedRate(this::cleanupExpiredGames, 5, 5, TimeUnit.MINUTES);
    }

    private void cleanupExpiredGames() {
        List<String> expiredGameIds = new ArrayList<>();
        
        for (MediaHuntGame game : activeGames.values()) {
            // Remove games that are expired or completed for more than 10 minutes
            if (game.isExpired() || 
                (game.getStatus() == MediaHuntGame.GameStatus.COMPLETED && 
                 game.getEndTime() != null && 
                 Instant.now().isAfter(game.getEndTime().plusSeconds(600)))) {
                expiredGameIds.add(game.getGameId());
            }
        }
        
        for (String gameId : expiredGameIds) {
            removeGame(gameId);
        }
        
        if (!expiredGameIds.isEmpty()) {
            System.out.println("Cleaned up " + expiredGameIds.size() + " expired games");
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    // Statistics methods
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalActiveGames", getActiveGameCount());
        stats.put("totalPlayers", getTotalPlayersInGames());
        stats.put("waitingGames", getWaitingGames().size());
        stats.put("inProgressGames", getInProgressGames().size());
        
        // Difficulty distribution
        Map<MediaHuntGame.GameDifficulty, Long> difficultyCount = new ConcurrentHashMap<>();
        for (MediaHuntGame.GameDifficulty difficulty : MediaHuntGame.GameDifficulty.values()) {
            long count = activeGames.values().stream()
                    .filter(game -> game.getDifficulty() == difficulty)
                    .count();
            difficultyCount.put(difficulty, count);
        }
        stats.put("difficultyDistribution", difficultyCount);
        
        return stats;
    }
}