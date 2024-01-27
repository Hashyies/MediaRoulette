package me.hash.mediaroulette.bot.commands;

import java.io.IOException;
import net.dv8tion.jda.api.Permission;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.Embeds;
import me.hash.mediaroulette.utils.random.RandomImage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ChannelNuke extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("nuke"))
            return;

        event.deferReply().queue();
        Bot.executor.execute(() -> {
            // Get the current time and the user's ID
            long now = System.currentTimeMillis();
            long userId = event.getUser().getIdLong();

            // Check if the user is on cooldown
            if (Bot.COOLDOWNS.containsKey(userId) && now - Bot.COOLDOWNS.get(userId) < Bot.COOLDOWN_DURATION) {
                // The user is on cooldown, reply with an embed and return
                Embeds.sendErrorEmbed(event, "Slow down dude", "Please wait for 2 seconds before using this command again!...");
                return;
            }

            // Update the user's cooldown
            Bot.COOLDOWNS.put(userId, now);

            // Check if the user has the "Manage Channel" permission
            if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                Embeds.sendErrorEmbed(event, "Sorry dude...", "You do not have the Manage Channel permission.");
                return;
            }

            // Check if the bot has the "Manage Channel" permission
            if (!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                Embeds.sendErrorEmbed(event, "Sorry dude...", "I do not have the Manage Channel permission.");
                return;
            }

            TextChannel oldChannel = event.getChannel().asTextChannel();
            int position = oldChannel.getPosition();

            // Create new channel
            TextChannel newChannel = oldChannel.createCopy().setPosition(position).complete();

            // Send embed message to new channel
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Channel Nuked");
            try {
                embedBuilder.setImage(RandomImage.getTenor("nuke").get("image"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            newChannel.sendMessageEmbeds(embedBuilder.build()).queue();

            // Delete old channel
            oldChannel.delete().queue();
        });
    }
}
