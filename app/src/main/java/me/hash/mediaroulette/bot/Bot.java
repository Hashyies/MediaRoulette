package me.hash.mediaroulette.bot;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.commands.*;
import me.hash.mediaroulette.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class Bot {

    static JDA jda = null;
    public static final long COOLDOWN_DURATION = 2500; // 2.5 seconds in milliseconds
    public static final Map<Long, Long> COOLDOWNS = new HashMap<>();
    public static  Config config = null;
    public static final ExecutorService executor = Executors.newCachedThreadPool();

    public Bot(String token) {
        jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("The Media Roulette"))
                .build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        jda.addEventListener(new getRandomImage(), new randomQuery(), new get4Chan(), new getPicsum(), new getReddit(), new getRule34xxx(), new ConfigCommand());

        jda.updateCommands().addCommands(
                Commands.slash("random", "Sends a random image")
                        .addOption(OptionType.BOOLEAN, "shouldcontinue",
                                "Should the bot keep generating images after 1?", false),
                Commands.slash("random-google", "Sends a random image from google")
                        .addOption(OptionType.STRING, "query",
                                "Image to search", true),
                Commands.slash("random-reddit", "Sends a random image from reddit")
                        .addOption(OptionType.STRING, "subreddit",
                                "Subreddit to get images from", false),
                Commands.slash("random-4chan", "Sends a random image from 4chan")
                        .addOption(OptionType.STRING, "board",
                                "Board to get images from", false),
                Commands.slash("random-picsum", "Sends a random image"),
                Commands.slash("random-rule34xxx", "Sends a random image"),
                Commands.slash("config", "Change personal, guild or bot settings")
                        .addSubcommands(new SubcommandData("bot", "Configure bot settings")
                                .addOption(OptionType.STRING, "option", "Field to change", true, true)
                                .addOption(OptionType.STRING, "value", "value to set", true))
        ).queue();

        config = new Config(Main.database);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(() -> {
                        if (!config.get("GENERATED_VOICE_CHANNEL", Boolean.class))
                                return;
                    GuildChannel voiceChannel = jda.getGuildChannelById(ChannelType.VOICE, 
                    Main.getEnv("GENERATED_VOICE_CHANNEL"));
                    if (voiceChannel != null) {
                        String imageGenerated = config.getOrDefault("image_generated", "0", String.class);

                        voiceChannel.getManager().setName("Generated: " + 
                         Config.formatBigInteger(new BigInteger(imageGenerated))).queue();
                    }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public JDA getJDA() {
        return jda;
    }

}