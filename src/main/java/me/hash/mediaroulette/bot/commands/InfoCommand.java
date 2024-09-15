package me.hash.mediaroulette.bot.commands;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.Config;
import me.hash.mediaroulette.utils.user.User;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class InfoCommand extends ListenerAdapter {
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
                // The user is on cooldown, reply with an embed and return
                Embeds.sendErrorEmbed(event, "Slow down dude",
                        "Please wait for 2 seconds before using this command again!...");
                return;
            }

            // Update the user's cooldown
            Bot.COOLDOWNS.put(userId, now);

            if (event.getSubcommandName().equals("bot")) {
                event.getHook().sendMessageEmbeds(getGlobalEmbed()).queue();
            } else if (event.getSubcommandName().equals("me")) {
                event.getHook().sendMessageEmbeds(getUserInfo(event.getUser().getId())).queue();
            }
        });
    }

    public MessageEmbed getGlobalEmbed() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Bot Information");
        embed.setColor(Color.BLUE);

        // Get the image generated value
        Config config = new Config(Main.database);
        String imageGenerated = config.getOrDefault("image_generated", "0", String.class);
        embed.addField("Images Generated", imageGenerated, true);

        // Calculate the uptime
        long uptimeMillis = System.currentTimeMillis() - Main.startTime;
        long uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis);
        long uptimeMinutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis);
        long uptimeHours = TimeUnit.MILLISECONDS.toHours(uptimeMillis);
        long uptimeDays = TimeUnit.MILLISECONDS.toDays(uptimeMillis);

        String uptime = String.format("%d days, %d hours, %d minutes, %d seconds",
                uptimeDays,
                uptimeHours - TimeUnit.DAYS.toHours(uptimeDays),
                uptimeMinutes - TimeUnit.HOURS.toMinutes(uptimeHours),
                uptimeSeconds - TimeUnit.MINUTES.toSeconds(uptimeMinutes));

        embed.addField("Uptime", uptime, true);

        // Send the embed
        return embed.build();
    }

    public MessageEmbed getUserInfo(String id) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Bot Information");
        embed.setColor(Color.BLUE);

        User user = User.get(Main.database, id);

        embed.addField("Images Generated", "" + user.getImagesGenerated(), true);
        embed.addField("Favorites used", user.getFavorites().size() + "/" + user.getFavoriteLimit(), true);
        embed.addField("Premium", "" + user.isPremium(), true);
        embed.addField("Admin", "" + user.isAdmin(), true);

        return embed.build();
    }

}
