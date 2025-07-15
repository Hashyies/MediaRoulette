package me.hash.mediaroulette.utils;

import me.hash.mediaroulette.model.minigame.MediaHuntGame;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaHuntManager {
    private static MediaHuntManager instance;
    private final Map<String, MediaHuntGame> activeGames; // gameId -> game
    private final Map<String, String> channelGames; // channelId -> gameId
    private final Map<String, String> playerGames; // playerId -> gameId
    private final ScheduledExecutorService scheduler;

    private MediaHuntManager() {
        this.activeGames = new ConcurrentHashMap<>();
        this.channelGames = new ConcurrentHashMap<>();
        this.playerGames = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Start cleanup task
        startCleanupTask();
    }

    public static MediaHuntManager getInstance() {
        if (instance == null) {
            synchronized (MediaHuntManager.class) {
                if (instance == null) {
                    instance = new MediaHuntManager();
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

        // Check if player is already in a game
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

        // Check if player is already in a game
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

    public boolean startGame(String gameId, String userId) {
        MediaHuntGame game = activeGames.get(gameId);
        if (game == null) return false;
        if (!game.getHostUserId().equals(userId)) return false;

        game.startGame();
        
        // Schedule automatic cleanup when game expires
        scheduler.schedule(() -> {
            MediaHuntGame expiredGame = activeGames.get(gameId);
            if (expiredGame != null && expiredGame.isExpired()) {
                expiredGame.setStatus(MediaHuntGame.GameStatus.EXPIRED);
                // Don't remove immediately, let players see results
                scheduler.schedule(() -> removeGame(gameId), 5, TimeUnit.MINUTES);
            }
        }, game.getDifficulty().getDurationSeconds() + 10, TimeUnit.SECONDS);

        return true;
    }

    public boolean submitTarget(String userId, MediaHuntGame.TargetType targetType, String evidence) {
        String gameId = playerGames.get(userId);
        if (gameId == null) return false;

        MediaHuntGame game = activeGames.get(gameId);
        if (game == null) return false;

        boolean submitted = game.submitTarget(userId, targetType, evidence);
        
        // If game completed, schedule cleanup
        if (game.getStatus() == MediaHuntGame.GameStatus.COMPLETED) {
            scheduler.schedule(() -> removeGame(gameId), 10, TimeUnit.MINUTES);
        }
        
        return submitted;
    }

    public MediaHuntGame getGameByPlayer(String userId) {
        String gameId = playerGames.get(userId);
        return gameId != null ? activeGames.get(gameId) : null;
    }

    public MediaHuntGame getGameByChannel(String channelId) {
        String gameId = channelGames.get(channelId);
        return gameId != null ? activeGames.get(gameId) : null;
    }

    public MediaHuntGame getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public List<MediaHuntGame> getActiveGames() {
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

    public boolean isPlayerInGame(String userId) {
        return playerGames.containsKey(userId);
    }

    public boolean isChannelInGame(String channelId) {
        return channelGames.containsKey(channelId);
    }

    private void removeGame(String gameId) {
        MediaHuntGame game = activeGames.remove(gameId);
        if (game != null) {
            // Remove from channel mapping
            channelGames.remove(game.getChannelId());
            
            // Remove all players from player mapping
            for (String playerId : game.getPlayerIds()) {
                playerGames.remove(playerId);
            }
        }
    }

    private void startCleanupTask() {
        // Run cleanup every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);
            
            List<String> gamesToRemove = new ArrayList<>();
            for (MediaHuntGame game : activeGames.values()) {
                // Remove games that are expired and old
                if ((game.getStatus() == MediaHuntGame.GameStatus.EXPIRED || 
                     game.getStatus() == MediaHuntGame.GameStatus.COMPLETED) &&
                    game.getEndTime() != null && 
                    game.getEndTime().isBefore(cutoff)) {
                    gamesToRemove.add(game.getGameId());
                }
                
                // Remove waiting games that are too old
                if (game.getStatus() == MediaHuntGame.GameStatus.WAITING_FOR_PLAYERS &&
                    game.getStartTime() != null &&
                    game.getStartTime().isBefore(cutoff)) {
                    gamesToRemove.add(game.getGameId());
                }
            }
            
            for (String gameId : gamesToRemove) {
                removeGame(gameId);
            }
            
        }, 5, 5, TimeUnit.MINUTES);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    // Statistics methods
    public int getTotalActiveGames() {
        return activeGames.size();
    }

    public int getTotalPlayers() {
        return playerGames.size();
    }

    public Map<MediaHuntGame.GameDifficulty, Integer> getGamesByDifficulty() {
        Map<MediaHuntGame.GameDifficulty, Integer> stats = new HashMap<>();
        for (MediaHuntGame.GameDifficulty difficulty : MediaHuntGame.GameDifficulty.values()) {
            stats.put(difficulty, 0);
        }
        
        for (MediaHuntGame game : activeGames.values()) {
            stats.put(game.getDifficulty(), stats.get(game.getDifficulty()) + 1);
        }
        
        return stats;
    }
}