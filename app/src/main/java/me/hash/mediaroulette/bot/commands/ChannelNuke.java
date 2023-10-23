package me.hash.mediaroulette.bot.commands;

import java.io.IOException;

import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.utils.random.RandomImage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ChannelNuke extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("nuke")) return;

        event.deferReply().queue();
        Bot.executor.execute(() -> {
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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            newChannel.sendMessageEmbeds(embedBuilder.build()).queue();

            // Delete old channel
            oldChannel.delete().queue();
        });
    }
}
