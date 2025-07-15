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

        event.deferReply(true).queue(); // Ephemeral reply

        Bot.executor.execute(() -> {
            try {
                String selectedTheme = event.getValues().get(0);
                User user = Main.userService.getOrCreateUser(event.getUser().getId());

                // Update user's theme
                user.setTheme(selectedTheme);
                Main.userService.updateUser(user);

                // Create success embed
                EmbedBuilder successEmbed = new EmbedBuilder()
                        .setTitle("🎨 Theme Updated!")
                        .setDescription("Your theme has been successfully changed to **" + formatThemeName(selectedTheme) + "**")
                        .setColor(SUCCESS_COLOR)
                        .setTimestamp(Instant.now());

                // Add theme preview
                Theme theme = themeManager.getTheme(selectedTheme);
                String themeDetails = String.format("```Name: %s\nDescription: %s\nEmoji: %s```",
                        formatThemeName(selectedTheme),
                        getThemeDescription(selectedTheme),
                        getThemeEmoji(selectedTheme));

                successEmbed.addField("📋 Theme Details", themeDetails, false);

                // Generate preview image of selected theme
                try {
                    byte[] previewImage = new ImageGenerator().generateImage("New Theme: " + formatThemeName(selectedTheme), selectedTheme);

                    if (previewImage.length > 0) {
                        InputStream imageStream = new ByteArrayInputStream(previewImage);
                        FileUpload imageUpload = FileUpload.fromData(imageStream, "new_theme_preview.png");

                        event.getHook().sendMessageEmbeds(successEmbed.build())
                                .addFiles(imageUpload)
                                .queue();
                    } else {
                        event.getHook().sendMessageEmbeds(successEmbed.build()).queue();
                    }
                } catch (Exception e) {
                    event.getHook().sendMessageEmbeds(successEmbed.build()).queue();
                }

            } catch (Exception e) {

                // Error embed
                EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ Theme Update Failed")
                        .setDescription("There was an error updating your theme. Please try again later.")
                        .setColor(new Color(255, 107, 107))
                        .setTimestamp(Instant.now());

                event.getHook().sendMessageEmbeds(errorEmbed.build()).queue();
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