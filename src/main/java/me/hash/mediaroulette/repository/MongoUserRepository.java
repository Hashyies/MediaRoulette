package me.hash.mediaroulette.repository;

import com.mongodb.client.MongoCollection;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.model.Favorite;
import me.hash.mediaroulette.model.ImageOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MongoUserRepository implements UserRepository {
    private final MongoCollection<Document> userCollection;

    public MongoUserRepository(MongoCollection<Document> userCollection) {
        this.userCollection = userCollection;
    }

    @Override
    public Optional<User> findById(String userId) {
        Document doc = userCollection.find(new Document("_id", userId)).first();
        if (doc != null) {
            User user = mapDocumentToUser(doc);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public User save(User user) {
        Document doc = mapUserToDocument(user);
        userCollection.replaceOne(new Document("_id", user.getUserId()), doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true));
        return user;
    }

    @Override
    public boolean exists(String userId) {
        return userCollection.find(new Document("_id", userId)).first() != null;
    }

    // --- Helper Methods for Mapping ---

    private User mapDocumentToUser(Document doc) {
        String userId = doc.getString("_id");
        User user = new User(userId);
        user.setImagesGenerated(doc.getLong("imagesGenerated") != null ? doc.getLong("imagesGenerated") : 0L);
        user.setNsfw(doc.getBoolean("nsfw", false));
        user.setPremium(doc.getBoolean("premium", false));
        user.setAdmin(doc.getBoolean("admin", false));
        user.setLocale(doc.getString("locale") != null ? doc.getString("locale") : "en_US");
        user.setTheme(doc.getString("theme") != null ? doc.getString("theme") : "default");
        
        // Map economy fields
        user.setCoins(doc.getLong("coins") != null ? doc.getLong("coins") : 100L);
        user.setTotalCoinsEarned(doc.getLong("totalCoinsEarned") != null ? doc.getLong("totalCoinsEarned") : 100L);
        user.setTotalCoinsSpent(doc.getLong("totalCoinsSpent") != null ? doc.getLong("totalCoinsSpent") : 0L);
        
        // Map last quest reset date
        String lastQuestResetStr = doc.getString("lastQuestReset");
        if (lastQuestResetStr != null) {
            user.setLastQuestReset(java.time.LocalDate.parse(lastQuestResetStr));
        }
        
        // Map quest completion tracking
        user.setTotalQuestsCompleted(doc.getLong("totalQuestsCompleted") != null ? doc.getLong("totalQuestsCompleted") : 0L);
        user.setQuestsCompletedToday(doc.getLong("questsCompletedToday") != null ? doc.getLong("questsCompletedToday") : 0L);
        
        String lastQuestCompletionDateStr = doc.getString("lastQuestCompletionDate");
        if (lastQuestCompletionDateStr != null) {
            user.setLastQuestCompletionDate(java.time.LocalDate.parse(lastQuestCompletionDateStr));
        }

        // Map favorites (assumes favorites is stored as a List of Documents)
        List<Document> favDocs = (List<Document>) doc.get("favorites", new ArrayList<Document>());
        for (Document favDoc : favDocs) {
            int id = favDoc.getInteger("id", 0);
            String description = favDoc.getString("description");
            String image = favDoc.getString("image");
            String type = favDoc.getString("type");
            user.getFavorites().add(new Favorite(id, description, image, type));
        }

        // Map quests
        List<Document> questDocs = (List<Document>) doc.get("dailyQuests", new ArrayList<Document>());
        for (Document questDoc : questDocs) {
            me.hash.mediaroulette.model.Quest quest = mapDocumentToQuest(questDoc);
            user.getDailyQuests().add(quest);
        }
        
        // Map transaction history
        List<Document> transactionDocs = (List<Document>) doc.get("transactionHistory", new ArrayList<Document>());
        for (Document transactionDoc : transactionDocs) {
            me.hash.mediaroulette.model.Transaction transaction = mapDocumentToTransaction(transactionDoc);
            user.getTransactionHistory().add(transaction);
        }

        // Map image options (assumes a sub-document "images")
        Document imagesDoc = (Document) doc.get("images");
        if (imagesDoc != null) {
            for (String key : imagesDoc.keySet()) {
                Document optionDoc = (Document) imagesDoc.get(key);
                boolean enabled = optionDoc.getBoolean("enabled", false);
                double chance = optionDoc.getDouble("chance") != null ? optionDoc.getDouble("chance") : 0.0;
                user.getImageOptionsMap().put(key, new ImageOptions(key, enabled, chance));
            }
        }
        return user;
    }

    private Document mapUserToDocument(User user) {
        Document doc = new Document("_id", user.getUserId())
                .append("imagesGenerated", user.getImagesGenerated())
                .append("nsfw", user.isNsfw())
                .append("premium", user.isPremium())
                .append("admin", user.isAdmin())
                .append("locale", user.getLocale())
                .append("theme", user.getTheme())
                .append("coins", user.getCoins())
                .append("totalCoinsEarned", user.getTotalCoinsEarned())
                .append("totalCoinsSpent", user.getTotalCoinsSpent())
                .append("lastQuestReset", user.getLastQuestReset() != null ? user.getLastQuestReset().toString() : null)
                .append("totalQuestsCompleted", user.getTotalQuestsCompleted())
                .append("questsCompletedToday", user.getQuestsCompletedToday())
                .append("lastQuestCompletionDate", user.getLastQuestCompletionDate() != null ? user.getLastQuestCompletionDate().toString() : null);

        // Map favorites
        List<Document> favDocs = new ArrayList<>();
        for (Favorite fav : user.getFavorites()) {
            Document favDoc = new Document("id", fav.getId())
                    .append("description", fav.getDescription())
                    .append("image", fav.getImage())
                    .append("type", fav.getType());
            favDocs.add(favDoc);
        }
        doc.append("favorites", favDocs);

        // Map quests
        List<Document> questDocs = new ArrayList<>();
        for (me.hash.mediaroulette.model.Quest quest : user.getDailyQuests()) {
            Document questDoc = mapQuestToDocument(quest);
            questDocs.add(questDoc);
        }
        doc.append("dailyQuests", questDocs);
        
        // Map transaction history
        List<Document> transactionDocs = new ArrayList<>();
        for (me.hash.mediaroulette.model.Transaction transaction : user.getTransactionHistory()) {
            Document transactionDoc = mapTransactionToDocument(transaction);
            transactionDocs.add(transactionDoc);
        }
        doc.append("transactionHistory", transactionDocs);

        // Map image options
        Document imagesDoc = new Document();
        for (Map.Entry<String, ImageOptions> entry : user.getImageOptionsMap().entrySet()) {
            ImageOptions option = entry.getValue();
            Document optionDoc = new Document("enabled", option.isEnabled())
                    .append("chance", option.getChance());
            imagesDoc.append(entry.getKey(), optionDoc);
        }
        doc.append("images", imagesDoc);
        return doc;
    }
    
    // Helper methods for Quest mapping
    private me.hash.mediaroulette.model.Quest mapDocumentToQuest(Document doc) {
        me.hash.mediaroulette.model.Quest quest = new me.hash.mediaroulette.model.Quest();
        quest.setQuestId(doc.getString("questId"));
        quest.setType(me.hash.mediaroulette.model.Quest.QuestType.valueOf(doc.getString("type")));
        quest.setDifficulty(me.hash.mediaroulette.model.Quest.QuestDifficulty.valueOf(doc.getString("difficulty")));
        quest.setTitle(doc.getString("title"));
        quest.setDescription(doc.getString("description"));
        quest.setEmoji(doc.getString("emoji"));
        quest.setTargetValue(doc.getInteger("targetValue", 0));
        quest.setCurrentProgress(doc.getInteger("currentProgress", 0));
        quest.setCoinReward(doc.getInteger("coinReward", 0));
        quest.setCompleted(doc.getBoolean("completed", false));
        quest.setClaimed(doc.getBoolean("claimed", false));
        
        String assignedDateStr = doc.getString("assignedDate");
        if (assignedDateStr != null) {
            quest.setAssignedDate(java.time.LocalDate.parse(assignedDateStr));
        }
        
        String completedAtStr = doc.getString("completedAt");
        if (completedAtStr != null) {
            quest.setCompletedAt(java.time.Instant.parse(completedAtStr));
        }
        
        quest.setMetadata(doc.getString("metadata"));
        return quest;
    }
    
    private Document mapQuestToDocument(me.hash.mediaroulette.model.Quest quest) {
        return new Document("questId", quest.getQuestId())
                .append("type", quest.getType().name())
                .append("difficulty", quest.getDifficulty().name())
                .append("title", quest.getTitle())
                .append("description", quest.getDescription())
                .append("emoji", quest.getEmoji())
                .append("targetValue", quest.getTargetValue())
                .append("currentProgress", quest.getCurrentProgress())
                .append("coinReward", quest.getCoinReward())
                .append("completed", quest.isCompleted())
                .append("claimed", quest.isClaimed())
                .append("assignedDate", quest.getAssignedDate() != null ? quest.getAssignedDate().toString() : null)
                .append("completedAt", quest.getCompletedAt() != null ? quest.getCompletedAt().toString() : null)
                .append("metadata", quest.getMetadata());
    }
    
    // Helper methods for Transaction mapping
    private me.hash.mediaroulette.model.Transaction mapDocumentToTransaction(Document doc) {
        me.hash.mediaroulette.model.Transaction transaction = new me.hash.mediaroulette.model.Transaction();
        transaction.setTransactionId(doc.getString("transactionId"));
        transaction.setUserId(doc.getString("userId"));
        transaction.setType(me.hash.mediaroulette.model.Transaction.TransactionType.valueOf(doc.getString("type")));
        transaction.setAmount(doc.getLong("amount"));
        transaction.setBalanceBefore(doc.getLong("balanceBefore"));
        transaction.setBalanceAfter(doc.getLong("balanceAfter"));
        transaction.setDescription(doc.getString("description"));
        
        String timestampStr = doc.getString("timestamp");
        if (timestampStr != null) {
            transaction.setTimestamp(java.time.Instant.parse(timestampStr));
        }
        
        return transaction;
    }
    
    private Document mapTransactionToDocument(me.hash.mediaroulette.model.Transaction transaction) {
        return new Document("transactionId", transaction.getTransactionId())
                .append("userId", transaction.getUserId())
                .append("type", transaction.getType().name())
                .append("amount", transaction.getAmount())
                .append("balanceBefore", transaction.getBalanceBefore())
                .append("balanceAfter", transaction.getBalanceAfter())
                .append("description", transaction.getDescription())
                .append("timestamp", transaction.getTimestamp() != null ? transaction.getTimestamp().toString() : null);
    }
}
