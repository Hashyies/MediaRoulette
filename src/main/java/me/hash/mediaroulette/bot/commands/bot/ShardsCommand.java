package me.hash.mediaroulette.bot.commands.bot;

import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ShardsCommand extends ListenerAdapter implements CommandHandler {

    @Override
    public CommandData getCommandData() {
        // Register the shard information command
        return Commands.slash("shards", "Get information about all shards.")
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("shards"))
            return;

        event.deferReply().queue(); // Defer the reply
        Bot.executor.execute(() -> {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Shard Information");
            embed.setColor(Color.CYAN);

            // Retrieves shard information from the ShardManager
            var shardManager = Bot.getShardManager();

            if (shardManager != null) {
                AtomicLong totalGuilds = new AtomicLong(); // Count guilds across shards

                String shardInfo = shardManager.getShards().stream()
                        .map(shard -> {
                            long shardId = shard.getShardInfo().getShardId();
                            long guilds = shard.getGuildCache().size(); // Guild count
                            long ping = shard.getGatewayPing(); // Shard ping
                            String status = shard.getStatus().name(); // Status

                            totalGuilds.addAndGet(guilds);
                            return String.format("Shard ID: `%d`\nStatus: **%s**\nPing: `%dms`\nGuilds: `%d`\n",
                                    shardId, status, ping, guilds);
                        }).collect(Collectors.joining("\n"));

                embed.setDescription(shardInfo);
                embed.addField("Total Shards", String.valueOf(shardManager.getShardsTotal()), true);
                embed.addField("Total Guilds", String.valueOf(totalGuilds.get()), true);

                // Send the embed
                event.getHook().sendMessageEmbeds(embed.build()).queue();
            }
        });
    }
}