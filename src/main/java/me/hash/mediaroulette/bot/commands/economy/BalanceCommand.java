package me.hash.mediaroulette.bot.commands.economy;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.utils.QuestGenerator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;

public class BalanceCommand extends ListenerAdapter implements CommandHandler {

    // Color scheme for economy commands
    private static final Color COIN_COLOR = new Color(255, 215, 0); // Gold
    private static final Color SUCCESS_COLOR = new Color(87, 242, 135); // Green
    private static final Color PREMIUM_COLOR = new Color(138, 43, 226); // Purple

    @Override
    public CommandData getCommandData() {
        return Commands.slash("balance", "💰 Check your coin balance and economy stats")
                .addOption(OptionType.USER, "user", "Check another user's balance (optional)")
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("balance")) return;

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

            // Determine which user to check
            net.dv8tion.jda.api.entities.User targetUser = event.getOption("user") != null
                    ? event.getOption("user").getAsUser()
                    : event.getUser();

            boolean isOwnBalance = targetUser.getId().equals(event.getUser().getId());

            // Track command usage
            Main.userService.trackCommandUsage(event.getUser().getId(), "balance");
            
            // Get user data
            User user = Main.userService.getOrCreateUser(targetUser.getId());

            // Update quest progress if checking another user's balance
            if (!isOwnBalance) {
                User currentUser = Main.userService.getOrCreateUser(event.getUser().getId());
                QuestGenerator.onBalanceChecked(currentUser, targetUser.getId());
                Main.userService.updateUser(currentUser);
            }

            // Create balance embed
            EmbedBuilder balanceEmbed = createBalanceEmbed(user, targetUser, isOwnBalance);

            event.getHook().sendMessageEmbeds(balanceEmbed.build()).queue();
        });
    }

    private EmbedBuilder createBalanceEmbed(User user, net.dv8tion.jda.api.entities.User discordUser, boolean isOwnBalance) {
        EmbedBuilder embed = new EmbedBuilder();

        // Set title and color based on premium status
        String title = isOwnBalance ? "💰 Your Balance" : "💰 " + discordUser.getName() + "'s Balance";
        Color embedColor = user.isPremium() ? PREMIUM_COLOR : COIN_COLOR;

        embed.setTitle(title);
        embed.setColor(embedColor);
        embed.setTimestamp(Instant.now());

        // Add user avatar
        if (discordUser.getAvatarUrl() != null) {
            embed.setThumbnail(discordUser.getAvatarUrl());
        }

        // Format numbers with commas
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);

        // Current balance (main field)
        String balanceText = String.format("```💰 %s coins```", formatter.format(user.getCoins()));
        embed.addField("💳 Current Balance", balanceText, true);

        // Images generated
        embed.addField("📸 Images Generated",
                String.format("```%s images```", formatter.format(user.getImagesGenerated())), true);

        // Premium status
        String statusText = user.isPremium() ? "```👑 Premium Member```" : "```🆓 Free Account```";
        embed.addField("🔰 Status", statusText, true);

        // Lifetime earnings
        embed.addField("📈 Total Earned",
                String.format("```💰 %s coins```", formatter.format(user.getTotalCoinsEarned())), true);

        // Total spent
        embed.addField("📉 Total Spent",
                String.format("```💰 %s coins```", formatter.format(user.getTotalCoinsSpent())), true);

        // Net worth
        embed.addField("💎 Net Worth",
                String.format("```💰 %s coins```", formatter.format(user.getNetWorth())), true);

        // Earning info
        embed.addField("💡 Earning Info",
                "```🎯 Complete daily quests to earn coins\n🛒 Visit /shop to spend your coins\n⭐ Premium users get exclusive benefits```", false);

        // Footer with tips
        if (isOwnBalance) {
            embed.setFooter("💡 Tip: Use /quests to see available daily quests! • Use /shop to spend coins", null);
        } else {
            embed.setFooter("Media Roulette • Economy System", null);
        }

        return embed;
    }
}