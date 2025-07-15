package me.hash.mediaroulette.model;

import me.hash.mediaroulette.exceptions.InvalidChancesException;
import me.hash.mediaroulette.exceptions.NoEnabledOptionsException;
import me.hash.mediaroulette.utils.user.ImageSelector;

import java.util.*;

public class User {
    public static final int DEFAULT_FAVORITE_LIMIT = 25;

    private String userId;
    private long imagesGenerated;
    private boolean nsfw;
    private boolean premium;
    private boolean admin;
    private List<Favorite> favorites;
    private Map<String, ImageOptions> imageOptions;
    private String locale; // locale support
    private String theme;
    private long coins; // User's currency balance
    private long totalCoinsEarned; // Total coins earned lifetime
    private long totalCoinsSpent; // Total coins spent lifetime
    private List<Quest> dailyQuests; // Current daily quests
    private List<Transaction> transactionHistory; // Transaction history for monitoring
    private java.time.LocalDate lastQuestReset; // Last time quests were reset
    private long totalQuestsCompleted; // Total number of quests completed lifetime
    private long questsCompletedToday; // Number of quests completed today
    private java.time.LocalDate lastQuestCompletionDate; // Last date a quest was completed

    public User(String userId) {
        this.userId = userId;
        this.imagesGenerated = 0;
        this.nsfw = false;
        this.premium = false;
        this.admin = false;
        this.favorites = new ArrayList<>();
        this.imageOptions = new HashMap<>();
        this.locale = "en_US"; // default locale
        this.coins = 100; // Starting balance
        this.totalCoinsEarned = 100; // Starting coins count as earned
        this.totalCoinsSpent = 0;
        this.dailyQuests = new ArrayList<>();
        this.transactionHistory = new ArrayList<>();
        this.lastQuestReset = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        this.totalQuestsCompleted = 0;
        this.questsCompletedToday = 0;
        this.lastQuestCompletionDate = null;
    }

    // --- Getters and Setters ---
    public String getUserId() { return userId; }
    public long getImagesGenerated() { return imagesGenerated; }
    public void setImagesGenerated(long imagesGenerated) { this.imagesGenerated = imagesGenerated; }
    public boolean isNsfw() { return nsfw; }
    public void setNsfw(boolean nsfw) { this.nsfw = nsfw; }
    public boolean isPremium() { return premium; }
    public void setPremium(boolean premium) { this.premium = premium; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    public List<Favorite> getFavorites() { return favorites; }
    public Map<String, ImageOptions> getImageOptionsMap() { return imageOptions; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public long getCoins() { return coins; }
    public void setCoins(long coins) { this.coins = coins; }
    public long getTotalCoinsEarned() { return totalCoinsEarned; }
    public void setTotalCoinsEarned(long totalCoinsEarned) { this.totalCoinsEarned = totalCoinsEarned; }
    public long getTotalCoinsSpent() { return totalCoinsSpent; }
    public void setTotalCoinsSpent(long totalCoinsSpent) { this.totalCoinsSpent = totalCoinsSpent; }
    public List<Quest> getDailyQuests() { return dailyQuests; }
    public void setDailyQuests(List<Quest> dailyQuests) { this.dailyQuests = dailyQuests; }
    public List<Transaction> getTransactionHistory() { return transactionHistory; }
    public void setTransactionHistory(List<Transaction> transactionHistory) { this.transactionHistory = transactionHistory; }
    public java.time.LocalDate getLastQuestReset() { return lastQuestReset; }
    public void setLastQuestReset(java.time.LocalDate lastQuestReset) { this.lastQuestReset = lastQuestReset; }
    public long getTotalQuestsCompleted() { return totalQuestsCompleted; }
    public void setTotalQuestsCompleted(long totalQuestsCompleted) { this.totalQuestsCompleted = totalQuestsCompleted; }
    public long getQuestsCompletedToday() { return questsCompletedToday; }
    public void setQuestsCompletedToday(long questsCompletedToday) { this.questsCompletedToday = questsCompletedToday; }
    public java.time.LocalDate getLastQuestCompletionDate() { return lastQuestCompletionDate; }
    public void setLastQuestCompletionDate(java.time.LocalDate lastQuestCompletionDate) { this.lastQuestCompletionDate = lastQuestCompletionDate; }

    // --- Business Logic Methods ---
    public void incrementImagesGenerated() {
        this.imagesGenerated++;
        // Note: Coins are now earned through quests, not automatic generation
    }

    // --- Currency Management Methods ---
    public Transaction addCoins(long amount, Transaction.TransactionType type, String description) {
        return addCoins(amount, type, description, null);
    }

    public Transaction addCoins(long amount, Transaction.TransactionType type, String description, String adminId) {
        if (amount <= 0) return null;
        
        long balanceBefore = this.coins;
        this.coins += amount;
        this.totalCoinsEarned += amount;
        
        Transaction transaction = new Transaction(this.userId, type, amount, balanceBefore, description);
        if (adminId != null) {
            transaction.setAdminId(adminId);
        }
        
        addTransaction(transaction);
        return transaction;
    }

    public Transaction spendCoins(long amount, Transaction.TransactionType type, String description) {
        return spendCoins(amount, type, description, null);
    }

    public Transaction spendCoins(long amount, Transaction.TransactionType type, String description, String adminId) {
        if (amount <= 0 || this.coins < amount) return null;
        
        long balanceBefore = this.coins;
        this.coins -= amount;
        this.totalCoinsSpent += amount;
        
        Transaction transaction = new Transaction(this.userId, type, -amount, balanceBefore, description);
        if (adminId != null) {
            transaction.setAdminId(adminId);
        }
        
        addTransaction(transaction);
        return transaction;
    }

    public boolean canAfford(long amount) {
        return this.coins >= amount;
    }

    public long getNetWorth() {
        return this.totalCoinsEarned - this.totalCoinsSpent;
    }

    private void addTransaction(Transaction transaction) {
        this.transactionHistory.add(transaction);
        
        // Keep only last 100 transactions to prevent memory issues
        if (this.transactionHistory.size() > 100) {
            this.transactionHistory.remove(0);
        }
        
        // Flag suspicious transactions
        flagSuspiciousActivity(transaction);
    }

    private void flagSuspiciousActivity(Transaction transaction) {
        // Flag large transactions
        if (Math.abs(transaction.getAmount()) > 10000) {
            transaction.setFlagged(true);
        }
        
        // Flag rapid transactions (more than 10 in last minute)
        long oneMinuteAgo = System.currentTimeMillis() - 60000;
        long recentTransactions = transactionHistory.stream()
                .filter(t -> t.getTimestamp().toEpochMilli() > oneMinuteAgo)
                .count();
        
        if (recentTransactions > 10) {
            transaction.setFlagged(true);
        }
    }

    // --- Quest Management Methods ---
    public void addQuest(Quest quest) {
        this.dailyQuests.add(quest);
    }

    public void updateQuestProgress(Quest.QuestType questType, int amount) {
        for (Quest quest : dailyQuests) {
            if (quest.getType() == questType && !quest.isCompleted()) {
                quest.addProgress(amount);
            }
        }
    }

    public List<Quest> getCompletedQuests() {
        return dailyQuests.stream()
                .filter(Quest::isCompleted)
                .toList();
    }

    public List<Quest> getClaimableQuests() {
        return dailyQuests.stream()
                .filter(Quest::canClaim)
                .toList();
    }

    public boolean needsQuestReset() {
        return lastQuestReset == null || 
               !lastQuestReset.equals(java.time.LocalDate.now(java.time.ZoneOffset.UTC));
    }

    public void resetDailyQuests() {
        this.dailyQuests.clear();
        this.lastQuestReset = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
    }

    public int getQuestLimit() {
        // Regular users get 2 quests (1 easy, 1 hard), premium users get 3 (1 easy, 1 hard, 1 premium)
        return isPremium() ? 3 : 2;
    }

    public Transaction claimQuestReward(Quest quest) {
        if (quest.canClaim()) {
            quest.claim();
            
            // Update quest completion tracking
            this.totalQuestsCompleted++;
            
            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
            if (this.lastQuestCompletionDate == null || !this.lastQuestCompletionDate.equals(today)) {
                // Reset daily count if it's a new day
                this.questsCompletedToday = 1;
                this.lastQuestCompletionDate = today;
            } else {
                // Same day, increment count
                this.questsCompletedToday++;
            }
            
            return addCoins(quest.getCoinReward(), Transaction.TransactionType.QUEST_REWARD, 
                    "Claimed reward for quest: " + quest.getTitle());
        }
        return null;
    }

    public int getFavoriteLimit() {
        return premium ? DEFAULT_FAVORITE_LIMIT * 2 : DEFAULT_FAVORITE_LIMIT;
    }

    public void addFavorite(String description, String image, String type) {
        if (favorites.size() >= getFavoriteLimit()) {
            // Log warning as needed – favorite limit reached.
            return;
        }
        int id = favorites.size();
        favorites.add(new Favorite(id, description, image, type));
    }

    public void removeFavorite(int id) {
        if (id < 0 || id >= favorites.size()) return;
        favorites.remove(id);
        // Reassign IDs so they remain sequential.
        for (int i = id; i < favorites.size(); i++) {
            favorites.get(i).setId(i);
        }
    }

    /**
     * Update (or set) image options with new chances.
     */
    public void setChances(ImageOptions... options) {
        for (ImageOptions option : options) {
            imageOptions.put(option.getImageType(), option);
        }
    }

    public ImageOptions getImageOptions(String imageType) {
        return imageOptions.get(imageType);
    }

    /**
     * Uses the ImageSelector to pick an image option.
     * @return a map containing the selected image details.
     * @throws NoEnabledOptionsException if no enabled options exist.
     * @throws InvalidChancesException if the chance values are invalid.
     */
    public Map<String, String> getImage() throws NoEnabledOptionsException, InvalidChancesException, me.hash.mediaroulette.exceptions.InvalidChancesException, me.hash.mediaroulette.exceptions.NoEnabledOptionsException {
        ImageSelector selector = new ImageSelector(imageOptions);
        return selector.selectImage(this.userId);
    }
}
