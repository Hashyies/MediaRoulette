package me.hash.mediaroulette.bot.commands.bot;

import java.awt.Color;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.errorHandler;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class InfoCommand extends ListenerAdapter implements CommandHandler {

    // Premium color palette
    private static final Color PRIMARY_COLOR = new Color(88, 101, 242); // Discord Blurple
    private static final Color SUCCESS_COLOR = new Color(87, 242, 135); // Green
    private static final Color PREMIUM_COLOR = new Color(255, 215, 0); // Gold
    private static final Color ACCENT_COLOR = new Color(114, 137, 218); // Light Blurple

    @Override
    public CommandData getCommandData() {
        return Commands.slash("info", "📊 Get detailed bot or user information")
                .addSubcommands(
                        new SubcommandData("bot", "🤖 Get comprehensive bot statistics and information"),
                        new SubcommandData("me", "👤 View your personal profile and usage statistics")
                )
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("info"))
            return;

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

            if (event.getSubcommandName().equals("bot")) {
                event.getHook().sendMessageEmbeds(getGlobalEmbed()).queue();
            } else if (event.getSubcommandName().equals("me")) {
                event.getHook().sendMessageEmbeds(getUserInfo(event.getUser().getId(), event.getUser().getName(), event.getUser().getAvatarUrl())).queue();
            }
        });
    }

    public MessageEmbed getGlobalEmbed() {
        EmbedBuilder embed = new EmbedBuilder();

        // Header with stylish title
        embed.setTitle("🤖 Media Roulette Bot");
        embed.setDescription("*Your premium AI-powered media generation companion*");
        embed.setColor(PRIMARY_COLOR);
        embed.setTimestamp(Instant.now());

        // Get real total images from all users
        long totalImages = Main.userService.getTotalImagesGenerated();
        long totalUsers = Main.userService.getTotalUsers();

        // Format the counts with commas for better readability
        String formattedImages = formatNumber(totalImages);
        String formattedUsers = formatNumber(totalUsers);

        // Calculate detailed uptime
        long uptimeMillis = System.currentTimeMillis() - Main.startTime;
        String formattedUptime = formatUptime(uptimeMillis);

        // Get real system metrics
        SystemMetrics metrics = getSystemMetrics();

        // Statistics section with emojis and formatting
        embed.addField("📈 **Total Images Generated**",
                "```" + formattedImages + "```", true);

        embed.addField("👥 **Total Users**",
                "```" + formattedUsers + "```", true);

        embed.addField("⏱️ **Bot Uptime**",
                "```" + formattedUptime + "```", true);

        embed.addField("🏓 **Gateway Ping**",
                "```" + Bot.getShardManager().getShards().getFirst().getGatewayPing() + "ms```", true);

        // Memory usage with progress bar
        String memoryUsage = String.format("```Used: %s\nMax: %s\n%s```",
                metrics.getUsedMemory(),
                metrics.getMaxMemory(),
                createProgressBar(metrics.getUsedMemoryBytes(), metrics.getMaxMemoryBytes(), 12));
        embed.addField("💾 **Memory Usage**", memoryUsage, true);

        // CPU usage
        embed.addField("⚡ **CPU Usage**",
                String.format("```%s\nCores: %d\nLoad: %s```",
                        metrics.getCpuUsage(),
                        metrics.getCpuCores(),
                        metrics.getSystemLoad()), true);

        // Disk usage
        embed.addField("💿 **Disk Usage**",
                String.format("```Used: %s\nFree: %s\nTotal: %s```",
                        metrics.getDiskUsed(),
                        metrics.getDiskFree(),
                        metrics.getDiskTotal()), true);

        // JVM Information
        embed.addField("☕ **JVM Info**",
                String.format("```Version: %s\nVendor: %s\nThreads: %d```",
                        System.getProperty("java.version"),
                        System.getProperty("java.vendor").split(" ")[0],
                        Thread.activeCount()), true);

        // Discord API Statistics
        embed.addField("🔗 **Discord Stats**",
                String.format("```Guilds: %d\nShards: %d```",
                        Bot.getShardManager().getShards().getFirst().getGuilds().size(),
                        Bot.getShardManager().getShards().getFirst().getShardInfo().getShardTotal()), true);

        // Footer with additional info
        embed.setFooter("Media Roulette • Real-time metrics", null);

        // Add bot avatar if available
        if (Bot.getShardManager().getShards().getFirst().getSelfUser().getAvatarUrl() != null) {
            embed.setThumbnail(Bot.getShardManager().getShards().getFirst().getSelfUser().getAvatarUrl());
        }

        return embed.build();
    }

    public MessageEmbed getUserInfo(String id, String username, String avatarUrl) {
        EmbedBuilder embed = new EmbedBuilder();

        User user = Main.userService.getOrCreateUser(id);

        // Dynamic color based on user status
        Color userColor = user.isPremium() ? PREMIUM_COLOR :
                user.isAdmin() ? new Color(220, 20, 60) : ACCENT_COLOR;

        embed.setColor(userColor);
        embed.setTimestamp(Instant.now());

        // User header with name and status badges
        StringBuilder titleBuilder = new StringBuilder("👤 " + username);
        if (user.isAdmin()) titleBuilder.append(" 🛡️");
        if (user.isPremium()) titleBuilder.append(" 👑");

        embed.setTitle(titleBuilder.toString());
        embed.setDescription("*Your personal Media Roulette profile*");

        // Set user avatar as thumbnail
        if (avatarUrl != null) {
            embed.setThumbnail(avatarUrl);
        }

        // User statistics with enhanced formatting
        String formattedImages = formatNumber(user.getImagesGenerated());
        embed.addField("🎨 **Images Generated**",
                "```" + formattedImages + "```", true);

        // Favorites with progress bar
        int favoritesUsed = user.getFavorites().size();
        int favoritesLimit = user.getFavoriteLimit();
        String favoritesBar = createProgressBar(favoritesUsed, favoritesLimit, 10);

        embed.addField("❤️ **Favorites**",
                String.format("```%d/%d\n%s```", favoritesUsed, favoritesLimit, favoritesBar), true);

        // Account status with badges
        StringBuilder statusBuilder = new StringBuilder("```");
        if (user.isPremium()) {
            statusBuilder.append("👑 Premium Member\n");
        } else {
            statusBuilder.append("🆓 Free Tier\n");
        }

        if (user.isAdmin()) {
            statusBuilder.append("🛡️ Administrator\n");
        }

        statusBuilder.append("✅ Account Active```");
        embed.addField("🏷️ **Account Status**", statusBuilder.toString(), true);

        // Usage statistics
        embed.addField("📊 **Usage Stats**",
                String.format("```📈 Generation Rate: %s\n⭐ Account Level: %s```",
                        getUsageLevel(user.getImagesGenerated()),
                        getAccountLevel(user)), true);

        // Quick actions or tips
        StringBuilder tipsBuilder = new StringBuilder("```");
        if (!user.isPremium()) {
            tipsBuilder.append("💡 Upgrade to Premium to higher the limit for favorite slots!\n");
        }
        if (favoritesUsed < favoritesLimit) {
            tipsBuilder.append("💾 You have ").append(favoritesLimit - favoritesUsed).append(" favorite slots available\n");
        }
        tipsBuilder.append("🎨 Keep getting random stuff!```");

        embed.addField("💡 **Tips & Info**", tipsBuilder.toString(), false);

        // Footer with join date or additional info
        embed.setFooter("Member since • Media Roulette", null);

        return embed.build();
    }

    // Helper method to format large numbers with commas
    private String formatNumber(long number) {
        return String.format("%,d", number);
    }

    // Helper method to format uptime in a readable way
    private String formatUptime(long uptimeMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis);
        long days = TimeUnit.MILLISECONDS.toDays(uptimeMillis);

        if (days > 0) {
            return String.format("%dd %dh %dm",
                    days,
                    hours - TimeUnit.DAYS.toHours(days),
                    minutes - TimeUnit.HOURS.toMinutes(hours));
        } else if (hours > 0) {
            return String.format("%dh %dm %ds",
                    hours,
                    minutes - TimeUnit.HOURS.toMinutes(hours),
                    seconds - TimeUnit.MINUTES.toSeconds(minutes));
        } else if (minutes > 0) {
            return String.format("%dm %ds",
                    minutes,
                    seconds - TimeUnit.MINUTES.toSeconds(minutes));
        } else {
            return String.format("%ds", seconds);
        }
    }

    // Helper method to create a visual progress bar
    private String createProgressBar(int current, int max, int length) {
        if (max == 0) return "░".repeat(length);

        int filled = (int) ((double) current / max * length);
        String bar = "█".repeat(Math.min(filled, length)) +
                "░".repeat(Math.max(0, length - filled));
        return bar;
    }

    // Overloaded version for long values (memory usage)
    private String createProgressBar(long current, long max, int length) {
        if (max == 0) return "░".repeat(length);

        int filled = (int) ((double) current / max * length);
        String bar = "█".repeat(Math.min(filled, length)) +
                "░".repeat(Math.max(0, length - filled));
        return bar;
    }

    // Helper method to determine usage level
    private String getUsageLevel(long imagesGenerated) {
        if (imagesGenerated >= 1000) return "Expert Creator";
        if (imagesGenerated >= 500) return "Advanced User";
        if (imagesGenerated >= 100) return "Regular User";
        if (imagesGenerated >= 10) return "Active User";
        return "New User";
    }

    // Helper method to determine account level
    private String getAccountLevel(User user) {
        if (user.isAdmin()) return "Administrator";
        if (user.isPremium()) return "Premium";
        return "Standard";
    }

    // System metrics helper class
    private SystemMetrics getSystemMetrics() {
        return new SystemMetrics();
    }

    // Inner class to handle system metrics
    private static class SystemMetrics {
        private final MemoryMXBean memoryBean;
        private final OperatingSystemMXBean osBean;
        private final Runtime runtime;
        private final File rootDir;

        public SystemMetrics() {
            this.memoryBean = ManagementFactory.getMemoryMXBean();
            this.osBean = ManagementFactory.getOperatingSystemMXBean();
            this.runtime = Runtime.getRuntime();
            this.rootDir = new File("/");
        }

        public String getUsedMemory() {
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            return formatBytes(usedMemory);
        }

        public String getMaxMemory() {
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            return maxMemory == -1 ? "Unlimited" : formatBytes(maxMemory);
        }

        public long getUsedMemoryBytes() {
            return memoryBean.getHeapMemoryUsage().getUsed();
        }

        public long getMaxMemoryBytes() {
            long max = memoryBean.getHeapMemoryUsage().getMax();
            return max == -1 ? runtime.maxMemory() : max;
        }

        public String getCpuUsage() {
            double cpuLoad = osBean.getSystemLoadAverage();
            if (cpuLoad < 0) {
                return "N/A";
            }
            return String.format("%.1f%%", cpuLoad * 100);
        }

        public int getCpuCores() {
            return osBean.getAvailableProcessors();
        }

        public String getSystemLoad() {
            double systemLoad = osBean.getSystemLoadAverage();
            if (systemLoad < 0) {
                return "N/A";
            }
            return String.format("%.2f", systemLoad);
        }

        public String getDiskUsed() {
            long total = rootDir.getTotalSpace();
            long free = rootDir.getFreeSpace();
            return formatBytes(total - free);
        }

        public String getDiskFree() {
            return formatBytes(rootDir.getFreeSpace());
        }

        public String getDiskTotal() {
            return formatBytes(rootDir.getTotalSpace());
        }

        public String getOsName() {
            String osName = System.getProperty("os.name");
            // Shorten common OS names
            if (osName.toLowerCase().contains("windows")) {
                return "Windows";
            } else if (osName.toLowerCase().contains("linux")) {
                return "Linux";
            } else if (osName.toLowerCase().contains("mac")) {
                return "macOS";
            }
            return osName.length() > 12 ? osName.substring(0, 12) + "..." : osName;
        }

        public String getOsArch() {
            return System.getProperty("os.arch");
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}