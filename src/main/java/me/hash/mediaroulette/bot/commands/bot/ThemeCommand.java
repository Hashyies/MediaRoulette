package me.hash.mediaroulette.bot.commands.bot;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Set;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.User;
import me.hash.mediaroulette.utils.media.image_generation.ImageGenerator;
import me.hash.mediaroulette.utils.media.image_generation.ThemeManager;
import me.hash.mediaroulette.utils.media.image_generation.Theme;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;

public class ThemeCommand extends ListenerAdapter implements CommandHandler {

    // Premium color palette
    private static final Color PRIMARY_COLOR = new Color(88, 101, 242); // Discord Blurple
    private static final Color SUCCESS_COLOR = new Color(87, 242, 135); // Green
    private static final Color PREMIUM_COLOR = new Color(255, 215, 0); // Gold
    private static final Color ACCENT_COLOR = new Color(114, 137, 218); // Light Blurple

    // Theme Manager instance
    private final ThemeManager themeManager;

    public ThemeCommand() {
        this.themeManager = ThemeManager.getInstance();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("theme", "🎨 Manage your personal theme settings")
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("theme"))
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

            // Get user data
            User user = Main.userService.getOrCreateUser(event.getUser().getId());
            String currentTheme = user.getTheme() != null ? user.getTheme() : "default";

            // Create the theme selection embed
            MessageEmbed themeEmbed = createThemeEmbed(user, currentTheme, event.getUser().getName(), event.getUser().getAvatarUrl());

            // Create the selection menu
            StringSelectMenu themeMenu = createThemeSelectionMenu(currentTheme);

            // Generate preview image of current theme
            try {
                byte[] previewImage = new ImageGenerator().generateImage("Current Theme: " + formatThemeName(currentTheme), currentTheme);

                if (previewImage.length > 0) {
                    InputStream imageStream = new ByteArrayInputStream(previewImage);
                    FileUpload imageUpload = FileUpload.fromData(imageStream, "theme_preview.png");

                    event.getHook().sendMessageEmbeds(themeEmbed)
                            .addFiles(imageUpload)
                            .addComponents(ActionRow.of(themeMenu))
                            .queue();
                } else {
                    // Fallback if image generation fails
                    event.getHook().sendMessageEmbeds(themeEmbed)
                            .addComponents(ActionRow.of(themeMenu))
                            .queue();
                }
            } catch (Exception e) {
                // Fallback if image generation fails
                event.getHook().sendMessageEmbeds(themeEmbed)
                        .addComponents(ActionRow.of(themeMenu))
                        .queue();
            }
        });
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("theme:selector"))
            return;

        event.deferEdit().queue(); // Defer edit instead of ephemeral reply

        Bot.executor.execute(() -> {
            try {
                String selectedTheme = event.getValues().get(0);
                User user = Main.userService.getOrCreateUser(event.getUser().getId());

                // Update user's theme
                user.setTheme(selectedTheme);
                Main.userService.updateUser(user);

                // Create updated theme embed with success message
                MessageEmbed updatedThemeEmbed = createUpdatedThemeEmbed(user, selectedTheme, event.getUser().getName(), event.getUser().getAvatarUrl());

                // Create updated selection menu with new current theme
                StringSelectMenu updatedThemeMenu = createThemeSelectionMenu(selectedTheme);

                // Generate preview image of selected theme
                try {
                    byte[] previewImage = new ImageGenerator().generateImage("Current Theme: " + formatThemeName(selectedTheme), selectedTheme);

                    if (previewImage.length > 0) {
                        InputStream imageStream = new ByteArrayInputStream(previewImage);
                        FileUpload imageUpload = FileUpload.fromData(imageStream, "theme_preview.png");

                        event.getHook().editOriginalEmbeds(updatedThemeEmbed)
                                .setFiles(imageUpload)
                                .setComponents(ActionRow.of(updatedThemeMenu))
                                .queue();
                    } else {
                        event.getHook().editOriginalEmbeds(updatedThemeEmbed)
                                .setComponents(ActionRow.of(updatedThemeMenu))
                                .queue();
                    }
                } catch (Exception e) {
                    event.getHook().editOriginalEmbeds(updatedThemeEmbed)
                            .setComponents(ActionRow.of(updatedThemeMenu))
                            .queue();
                }

            } catch (Exception e) {

                // Error embed
                EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ Theme Update Failed")
                        .setDescription("There was an error updating your theme. Please try again later.")
                        .setColor(new Color(255, 107, 107))
                        .setTimestamp(Instant.now());

                event.getHook().editOriginalEmbeds(errorEmbed.build()).queue();
            }
        });
    }

    private MessageEmbed createThemeEmbed(User user, String currentTheme, String username, String avatarUrl) {
        EmbedBuilder embed = new EmbedBuilder();

        // Dynamic color based on user status
        Color userColor = user.isPremium() ? PREMIUM_COLOR :
                user.isAdmin() ? new Color(220, 20, 60) : ACCENT_COLOR;

        embed.setColor(userColor);
        embed.setTimestamp(Instant.now());

        // Header with user info
        StringBuilder titleBuilder = new StringBuilder("🎨 Theme Settings - " + username);
        if (user.isAdmin()) titleBuilder.append(" 🛡️");
        if (user.isPremium()) titleBuilder.append(" 👑");

        embed.setTitle(titleBuilder.toString());
        embed.setDescription("*Customize your Media Roulette experience with themes*");

        // Set user avatar as thumbnail
        if (avatarUrl != null) {
            embed.setThumbnail(avatarUrl);
        }

        // Current theme information
        embed.addField("🎯 **Current Theme**",
                String.format("```%s %s\n%s```",
                        getThemeEmoji(currentTheme),
                        formatThemeName(currentTheme),
                        getThemeDescription(currentTheme)), true);

        // Theme stats
        int availableThemes = themeManager.getThemeCount();
        embed.addField("📊 **Available Themes**",
                String.format("```Total: %d themes\nUnlocked: %s```",
                        availableThemes,
                        user.isPremium() ? "All themes" : "Basic themes"), true);

        // User status
        StringBuilder statusBuilder = new StringBuilder("```");
        if (user.isPremium()) {
            statusBuilder.append("👑 Premium Access\n✨ All themes unlocked");
        } else {
            statusBuilder.append("🆓 Free Access\n🔒 Some themes locked");
        }
        statusBuilder.append("```");
        embed.addField("🏷️ **Access Level**", statusBuilder.toString(), true);

        // Instructions
        embed.addField("💡 **How to Use**",
                "```🔽 Use the dropdown menu below to select a new theme\n📸 Preview images show how your theme will look\n💾 Changes are saved automatically```",
                false);

        // Premium advertisement for free users
        if (!user.isPremium()) {
            embed.addField("⭐ **Upgrade to Premium**",
                    "```✨ Unlock all premium themes\n🎨 Get exclusive color schemes\n🚀 Priority theme updates```",
                    false);
        }

        // Footer
        embed.setFooter("Media Roulette • Theme Customization", null);

        // Set the generated theme preview as image
        embed.setImage("attachment://theme_preview.png");

        return embed.build();
    }

    private MessageEmbed createUpdatedThemeEmbed(User user, String selectedTheme, String username, String avatarUrl) {
        EmbedBuilder embed = new EmbedBuilder();

        // Header with success message
        embed.setTitle("🎨 Theme Updated Successfully!");
        embed.setColor(SUCCESS_COLOR);
        embed.setTimestamp(Instant.now());

        // Add user info
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            embed.setAuthor(username + "'s Theme Settings", null, avatarUrl);
        } else {
            embed.setAuthor(username + "'s Theme Settings");
        }

        embed.setDescription("*Your theme has been changed to **" + formatThemeName(selectedTheme) + "***");

        // Current theme information
        embed.addField("🎨 Current Theme",
                String.format("```%s %s```",
                        getThemeEmoji(selectedTheme),
                        formatThemeName(selectedTheme)), true);

        // Theme statistics
        int availableThemes = themeManager.getThemeCount();
        embed.addField("📊 Theme Stats",
                String.format("```Total: %d themes\nUnlocked: %s```",
                        availableThemes,
                        user.isPremium() ? "All themes" : "Basic themes"), true);

        // User status
        StringBuilder statusBuilder = new StringBuilder();
        if (user.isPremium()) {
            statusBuilder.append("👑 Premium Access\n✨ All themes unlocked");
        } else {
            statusBuilder.append("🆓 Free Access\n🔒 Some themes locked");
        }
        embed.addField("🔰 Account Status", statusBuilder.toString(), true);

        // Instructions
        embed.addField("📖 Instructions",
                "```🔽 Use the dropdown menu below to select a different theme\n📸 Preview images show how your theme will look\n💾 Changes are saved automatically```",
                false);

        // Premium promotion for non-premium users
        if (!user.isPremium()) {
            embed.addField("⭐ Upgrade to Premium",
                    "```✨ Unlock all premium themes\n🎨 Get exclusive color schemes\n🚀 Priority theme updates```",
                    false);
        }

        // Set footer
        embed.setFooter("Media Roulette • Theme System", null);

        // Set the generated theme preview as image
        embed.setImage("attachment://theme_preview.png");

        return embed.build();
    }

    private StringSelectMenu createThemeSelectionMenu(String currentTheme) {
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("theme:selector")
                .setPlaceholder("🎨 Choose a theme...")
                .setRequiredRange(1, 1);

        // Get available themes from ThemeManager
        Set<String> availableThemes = themeManager.getAvailableThemeNames();

        // Add theme options
        for (String themeName : availableThemes) {
            String emoji = getThemeEmoji(themeName);
            String description = getThemeDescription(themeName);

            // Mark current theme as default
            boolean isCurrentTheme = themeName.equals(currentTheme);

            menuBuilder.addOption(
                    emoji + " " + formatThemeName(themeName) + (isCurrentTheme ? " (Current)" : ""),
                    themeName,
                    description
            );
        }

        return menuBuilder.build();
    }

    // Helper method to format theme names (capitalize first letter)
    private String formatThemeName(String themeName) {
        if (themeName == null || themeName.isEmpty()) {
            return "Default";
        }
        return themeName.substring(0, 1).toUpperCase() + themeName.substring(1).toLowerCase();
    }

    // Helper method to get theme description based on actual themes
    private String getThemeDescription(String themeName) {
        return switch (themeName.toLowerCase()) {
            case "default" -> "Clean and modern default theme with blue accents";
            case "dark" -> "Sleek dark theme for low-light environments";
            case "neon" -> "Vibrant cyberpunk theme with electric colors";
            case "minimal" -> "Clean minimalist design with subtle shadows";
            default -> "Custom theme with unique styling";
        };
    }

    // Helper method to get theme emoji based on actual themes
    private String getThemeEmoji(String themeName) {
        switch (themeName.toLowerCase()) {
            case "default":
                return "🔵";
            case "dark":
                return "⚫";
            case "neon":
                return "🌈";
            case "minimal":
                return "⚪";
            default:
                return "🎨";
        }
    }

}