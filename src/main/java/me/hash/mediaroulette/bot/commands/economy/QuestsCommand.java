package me.hash.mediaroulette.bot.commands.economy;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.Quest;
import me.hash.mediaroulette.model.Transaction;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.utils.QuestGenerator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class QuestsCommand extends ListenerAdapter implements CommandHandler {

    // Color scheme for quest difficulties
    private static final Color EASY_COLOR = new Color(87, 242, 135); // Green
    private static final Color HARD_COLOR = new Color(255, 165, 0); // Orange
    private static final Color PREMIUM_COLOR = new Color(138, 43, 226); // Purple
    private static final Color COMPLETED_COLOR = new Color(255, 215, 0); // Gold

    @Override
    public CommandData getCommandData() {
        return Commands.slash("quests", "🎯 View and manage your daily quests")
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("quests")) return;

        event.deferReply().queue();
        Bot.executor.execute(() -> {
            // Get the current time and the user's ID
            long now = System.currentTimeMillis();
            long userId = event.getUser().getIdLong();

            // Check if the user is on cooldown
            if (Bot.COOLDOWNS.containsKey(userId) && now - Bot.COOLDOWNS.get(userId) < Bot.COOLDOWN_DURATION) {
                // Enhanced cooldown message
                EmbedBuilder cooldownEmbed = new EmbedBuilder()
                        .setTitle("⏰ Slow Down!")
                        .setDescription("Please wait **2 seconds** before using this command again.")
                        .setColor(new Color(255, 107, 107))
                        .setTimestamp(Instant.now());

                event.getHook().sendMessageEmbeds(cooldownEmbed.build()).queue();
                return;
            }

            // Update the user's cooldown
            Bot.COOLDOWNS.put(userId, now);

            // Get user data
            User user = Main.userService.getOrCreateUser(event.getUser().getId());

            // Check if user needs quest reset or has no quests
            if (user.needsQuestReset() || user.getDailyQuests().isEmpty()) {
                resetUserQuests(user);
            }

            // Create quest embed
            EmbedBuilder questEmbed = createQuestEmbed(user, event.getUser());

            // Create action buttons
            List<Button> buttons = createQuestButtons(user);

            if (buttons.isEmpty()) {
                event.getHook().sendMessageEmbeds(questEmbed.build()).queue();
            } else {
                event.getHook().sendMessageEmbeds(questEmbed.build())
                        .addComponents(ActionRow.of(buttons))
                        .queue();
            }
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("quest:")) return;

        event.deferEdit().queue();
        Bot.executor.execute(() -> {
            User user = Main.userService.getOrCreateUser(event.getUser().getId());
            String action = event.getComponentId().split(":")[1];

            switch (action) {
                case "claim_all" -> handleClaimAll(event, user);
                case "refresh" -> handleRefresh(event, user);
                default -> {
                    if (action.startsWith("claim_")) {
                        String questId = action.substring(6);
                        handleClaimQuest(event, user, questId);
                    }
                }
            }
        });
    }

    private void resetUserQuests(User user) {
        user.resetDailyQuests();
        List<Quest> newQuests = QuestGenerator.generateDailyQuests(user);
        newQuests.forEach(user::addQuest);
        Main.userService.updateUser(user);
    }

    private EmbedBuilder createQuestEmbed(User user, net.dv8tion.jda.api.entities.User discordUser) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("🎯 Daily Quests");
        embed.setColor(user.isPremium() ? PREMIUM_COLOR : EASY_COLOR);
        embed.setTimestamp(Instant.now());

        // Add user avatar
        if (discordUser.getAvatarUrl() != null) {
            embed.setThumbnail(discordUser.getAvatarUrl());
        }

        // Quest reset info
        String resetTime = user.getLastQuestReset()
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")) + " UTC";
        
        embed.setDescription("*Complete quests to earn coins! Quests reset daily.*\n" +
                "**Next Reset:** " + resetTime);

        // Add quest progress
        List<Quest> quests = user.getDailyQuests();
        if (quests.isEmpty()) {
            embed.addField("📋 No Quests Available", 
                    "```Your daily quests will be generated automatically.\nTry running the command again!```", false);
        } else {
            for (int i = 0; i < quests.size(); i++) {
                Quest quest = quests.get(i);
                embed.addField(createQuestFieldTitle(quest), createQuestFieldValue(quest), false);
            }
        }

        // Quest statistics
        long completedToday = quests.stream().filter(Quest::isCompleted).count();
        long claimableRewards = quests.stream().filter(Quest::canClaim).mapToLong(Quest::getCoinReward).sum();

        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        embed.addField("📊 Today's Progress", 
                String.format("```Completed: %d/%d quests\nClaimable Rewards: %s coins```", 
                        completedToday, quests.size(), formatter.format(claimableRewards)), false);

        embed.setFooter("💡 Tip: Complete quests daily for maximum coin earnings!", null);

        return embed;
    }

    private String createQuestFieldTitle(Quest quest) {
        Color difficultyColor = getDifficultyColor(quest.getDifficulty());
        String difficultyName = quest.getDifficulty().name();
        
        return String.format("%s %s %s [%s]", 
                quest.getStatusEmoji(), 
                quest.getEmoji(), 
                quest.getTitle(),
                difficultyName);
    }

    private String createQuestFieldValue(Quest quest) {
        StringBuilder value = new StringBuilder();
        
        // Description
        value.append("```").append(quest.getDescription()).append("```");
        
        // Progress bar
        value.append("**Progress:** ").append(quest.getCurrentProgress())
              .append("/").append(quest.getTargetValue()).append("\n");
        value.append("```").append(quest.getProgressBar()).append("```");
        
        // Reward and status
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        if (quest.isClaimed()) {
            value.append("✅ **Claimed:** ").append(formatter.format(quest.getCoinReward())).append(" coins");
        } else if (quest.isCompleted()) {
            value.append("🎁 **Ready to claim:** ").append(formatter.format(quest.getCoinReward())).append(" coins");
        } else {
            value.append("💰 **Reward:** ").append(formatter.format(quest.getCoinReward())).append(" coins");
        }
        
        return value.toString();
    }

    private Color getDifficultyColor(Quest.QuestDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> EASY_COLOR;
            case HARD -> HARD_COLOR;
            case PREMIUM -> PREMIUM_COLOR;
        };
    }

    private List<Button> createQuestButtons(User user) {
        List<Button> buttons = new java.util.ArrayList<>();
        
        // Add claim buttons for individual quests
        List<Quest> claimableQuests = user.getClaimableQuests();
        if (!claimableQuests.isEmpty()) {
            if (claimableQuests.size() == 1) {
                Quest quest = claimableQuests.get(0);
                buttons.add(Button.success("quest:claim_" + quest.getQuestId(), 
                        "Claim " + quest.getCoinReward() + " coins")
                        .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("🎁")));
            } else {
                // Multiple claimable quests - add claim all button
                long totalReward = claimableQuests.stream().mapToLong(Quest::getCoinReward).sum();
                buttons.add(Button.success("quest:claim_all", 
                        "Claim All (" + totalReward + " coins)")
                        .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("🎁")));
            }
        }
        
        // Add refresh button
        buttons.add(Button.secondary("quest:refresh", "Refresh")
                .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("🔄")));
        
        return buttons;
    }

    private void handleClaimAll(ButtonInteractionEvent event, User user) {
        List<Quest> claimableQuests = user.getClaimableQuests();
        if (claimableQuests.isEmpty()) {
            updateQuestDisplay(event, user, "❌ No quests available to claim!");
            return;
        }

        long totalReward = 0;
        for (Quest quest : claimableQuests) {
            Transaction transaction = user.claimQuestReward(quest);
            if (transaction != null) {
                totalReward += transaction.getAmount();
            }
        }

        Main.userService.updateUser(user);
        
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        updateQuestDisplay(event, user, 
                String.format("🎉 Claimed %s coins from %d quest(s)!", 
                        formatter.format(totalReward), claimableQuests.size()));
    }

    private void handleClaimQuest(ButtonInteractionEvent event, User user, String questId) {
        Quest quest = user.getDailyQuests().stream()
                .filter(q -> q.getQuestId().equals(questId))
                .findFirst()
                .orElse(null);

        if (quest == null || !quest.canClaim()) {
            updateQuestDisplay(event, user, "❌ Quest not found or not claimable!");
            return;
        }

        Transaction transaction = user.claimQuestReward(quest);
        Main.userService.updateUser(user);

        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        updateQuestDisplay(event, user, 
                String.format("🎉 Claimed %s coins for completing: %s", 
                        formatter.format(transaction.getAmount()), quest.getTitle()));
    }

    private void handleRefresh(ButtonInteractionEvent event, User user) {
        updateQuestDisplay(event, user, null);
    }

    private void updateQuestDisplay(ButtonInteractionEvent event, User user, String message) {
        EmbedBuilder questEmbed = createQuestEmbed(user, event.getUser());
        
        if (message != null) {
            questEmbed.addField("📢 Update", message, false);
        }

        List<Button> buttons = createQuestButtons(user);

        if (buttons.isEmpty()) {
            event.getHook().editOriginalEmbeds(questEmbed.build())
                    .setComponents()
                    .queue();
        } else {
            event.getHook().editOriginalEmbeds(questEmbed.build())
                    .setComponents(ActionRow.of(buttons))
                    .queue();
        }
    }
}