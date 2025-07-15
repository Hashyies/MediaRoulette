package me.hash.mediaroulette.model.minigame;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MediaHuntGame {
    public enum GameDifficulty {
        EASY(3, 120, 50, 100),      // 3 targets, 2 minutes, 50-100 coins
        MEDIUM(5, 180, 100, 200),   // 5 targets, 3 minutes, 100-200 coins
        HARD(7, 240, 200, 400),     // 7 targets, 4 minutes, 200-400 coins
        EXTREME(10, 300, 400, 800); // 10 targets, 5 minutes, 400-800 coins

        private final int targetCount;
        private final int durationSeconds;
        private final int minReward;
        private final int maxReward;

        GameDifficulty(int targetCount, int durationSeconds, int minReward, int maxReward) {
            this.targetCount = targetCount;
            this.durationSeconds = durationSeconds;
            this.minReward = minReward;
            this.maxReward = maxReward;
        }

        public int getTargetCount() { return targetCount; }
        public int getDurationSeconds() { return durationSeconds; }
        public int getMinReward() { return minReward; }
        public int getMaxReward() { return maxReward; }
    }

    public enum GameStatus {
        WAITING_FOR_PLAYERS,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        EXPIRED
    }

    public enum TargetType {
        REDDIT_POST("Find a Reddit post", "🔴", "reddit"),
        IMGUR_IMAGE("Find an Imgur image", "🟢", "imgur"),
        PICSUM_PHOTO("Find a Picsum photo", "🔵", "picsum"),
        MOVIE_POSTER("Find a movie poster", "🎬", "movie"),
        TV_SHOW("Find a TV show", "📺", "tvshow"),
        YOUTUBE_VIDEO("Find a YouTube video", "📹", "youtube"),
        MEME_IMAGE("Find a meme/funny image", "😂", "all"),
        NATURE_PHOTO("Find a nature photo", "🌿", "picsum"),
        URBAN_DEFINITION("Find an Urban Dictionary word", "📖", "urban");

        private final String description;
        private final String emoji;
        private final String sourceHint;

        TargetType(String description, String emoji, String sourceHint) {
            this.description = description;
            this.emoji = emoji;
            this.sourceHint = sourceHint;
        }

        public String getDescription() { return description; }
        public String getEmoji() { return emoji; }
        public String getSourceHint() { return sourceHint; }
    }

    private String gameId;
    private String hostUserId;
    private String channelId;
    private GameDifficulty difficulty;
    private GameStatus status;
    private List<String> playerIds;
    private List<TargetType> targets;
    private Map<TargetType, String> completedTargets; // Target -> Player who found it
    private Map<String, Integer> playerContributions; // Player -> number of targets found
    private Instant startTime;
    private Instant endTime;
    private int maxPlayers;
    private long totalReward;

    public MediaHuntGame() {
        this.gameId = generateGameId();
        this.playerIds = new ArrayList<>();
        this.targets = new ArrayList<>();
        this.completedTargets = new ConcurrentHashMap<>();
        this.playerContributions = new ConcurrentHashMap<>();
        this.status = GameStatus.WAITING_FOR_PLAYERS;
        this.maxPlayers = 6; // Maximum 6 players per game
    }

    public MediaHuntGame(String hostUserId, String channelId, GameDifficulty difficulty) {
        this();
        this.hostUserId = hostUserId;
        this.channelId = channelId;
        this.difficulty = difficulty;
        this.playerIds.add(hostUserId);
        this.playerContributions.put(hostUserId, 0);
        generateTargets();
        calculateReward();
    }

    private String generateGameId() {
        return "HUNT_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    private void generateTargets() {
        List<TargetType> availableTargets = new ArrayList<>(Arrays.asList(TargetType.values()));
        Collections.shuffle(availableTargets);
        
        for (int i = 0; i < Math.min(difficulty.getTargetCount(), availableTargets.size()); i++) {
            targets.add(availableTargets.get(i));
        }
    }

    private void calculateReward() {
        int baseReward = difficulty.getMinReward() + 
                (int)(Math.random() * (difficulty.getMaxReward() - difficulty.getMinReward()));
        this.totalReward = baseReward;
    }

    public boolean addPlayer(String userId) {
        if (status != GameStatus.WAITING_FOR_PLAYERS) return false;
        if (playerIds.size() >= maxPlayers) return false;
        if (playerIds.contains(userId)) return false;

        playerIds.add(userId);
        playerContributions.put(userId, 0);
        return true;
    }

    public boolean removePlayer(String userId) {
        if (status != GameStatus.WAITING_FOR_PLAYERS) return false;
        if (userId.equals(hostUserId) && playerIds.size() > 1) {
            // Transfer host to next player
            playerIds.remove(userId);
            hostUserId = playerIds.get(0);
        } else {
            playerIds.remove(userId);
        }
        playerContributions.remove(userId);
        return true;
    }

    public void startGame() {
        if (status != GameStatus.WAITING_FOR_PLAYERS) return;
        if (playerIds.isEmpty()) return;

        this.status = GameStatus.IN_PROGRESS;
        this.startTime = Instant.now();
        this.endTime = startTime.plus(difficulty.getDurationSeconds(), ChronoUnit.SECONDS);
    }

    public boolean submitTarget(String userId, TargetType targetType, String evidence) {
        if (status != GameStatus.IN_PROGRESS) return false;
        if (!playerIds.contains(userId)) return false;
        if (completedTargets.containsKey(targetType)) return false;
        if (!targets.contains(targetType)) return false;
        if (isExpired()) {
            status = GameStatus.EXPIRED;
            return false;
        }

        completedTargets.put(targetType, userId);
        playerContributions.put(userId, playerContributions.getOrDefault(userId, 0) + 1);

        // Check if game is completed
        if (completedTargets.size() >= targets.size()) {
            status = GameStatus.COMPLETED;
            endTime = Instant.now();
        }

        return true;
    }

    public boolean isExpired() {
        return endTime != null && Instant.now().isAfter(endTime);
    }

    public boolean isActive() {
        return status == GameStatus.IN_PROGRESS && !isExpired();
    }

    public long getTimeRemainingSeconds() {
        if (endTime == null) return 0;
        return Math.max(0, ChronoUnit.SECONDS.between(Instant.now(), endTime));
    }

    public double getProgressPercentage() {
        if (targets.isEmpty()) return 0.0;
        return (double) completedTargets.size() / targets.size() * 100.0;
    }

    public String getProgressBar() {
        int totalBars = 10;
        int filledBars = (int) (getProgressPercentage() / 10);
        StringBuilder bar = new StringBuilder();
        
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                bar.append("🟩");
            } else {
                bar.append("⬜");
            }
        }
        
        return bar.toString();
    }

    public Map<String, Long> calculateRewards() {
        Map<String, Long> rewards = new HashMap<>();
        
        if (status != GameStatus.COMPLETED) {
            // Game not completed, no rewards
            return rewards;
        }

        // Base reward for participation
        long baseReward = totalReward / 3;
        
        // Performance bonus based on contributions
        long performancePool = totalReward - (baseReward * playerIds.size());
        
        for (String playerId : playerIds) {
            long playerReward = baseReward;
            
            // Add performance bonus
            int contributions = playerContributions.getOrDefault(playerId, 0);
            if (completedTargets.size() > 0) {
                double contributionRatio = (double) contributions / completedTargets.size();
                playerReward += (long) (performancePool * contributionRatio);
            }
            
            rewards.put(playerId, playerReward);
        }
        
        return rewards;
    }

    public List<TargetType> getRemainingTargets() {
        return targets.stream()
                .filter(target -> !completedTargets.containsKey(target))
                .toList();
    }

    public List<TargetType> getCompletedTargetsList() {
        return new ArrayList<>(completedTargets.keySet());
    }

    public String getTopContributor() {
        return playerContributions.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public String getGameSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("**Game Summary:**\n");
        summary.append("Difficulty: ").append(difficulty.name()).append("\n");
        summary.append("Players: ").append(playerIds.size()).append("\n");
        summary.append("Targets Found: ").append(completedTargets.size()).append("/").append(targets.size()).append("\n");
        summary.append("Status: ").append(status.name()).append("\n");
        
        if (status == GameStatus.COMPLETED) {
            summary.append("Total Reward: ").append(totalReward).append(" coins\n");
            String topPlayer = getTopContributor();
            if (topPlayer != null) {
                summary.append("Top Contributor: <@").append(topPlayer).append(">\n");
            }
        }
        
        return summary.toString();
    }

    // Getters and Setters
    public String getGameId() { return gameId; }
    public String getHostUserId() { return hostUserId; }
    public String getChannelId() { return channelId; }
    public GameDifficulty getDifficulty() { return difficulty; }
    public GameStatus getStatus() { return status; }
    public List<String> getPlayerIds() { return new ArrayList<>(playerIds); }
    public List<TargetType> getTargets() { return new ArrayList<>(targets); }
    public Map<TargetType, String> getCompletedTargets() { return new HashMap<>(completedTargets); }
    public Map<String, Integer> getPlayerContributions() { return new HashMap<>(playerContributions); }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public int getMaxPlayers() { return maxPlayers; }
    public long getTotalReward() { return totalReward; }

    public void setStatus(GameStatus status) { this.status = status; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
}