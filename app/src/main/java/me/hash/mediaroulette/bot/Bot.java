package me.hash.mediaroulette.bot;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.commands.*;
import me.hash.mediaroulette.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Bot {
    static JDA jda = null;
    public static final long COOLDOWN_DURATION = 2500; // 2.5 seconds in milliseconds
    public static final Map<Long, Long> COOLDOWNS = new HashMap<>();
    public static Config config = null;
    public static final ExecutorService executor = Executors.newCachedThreadPool();

    public Bot(String token) {
        jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("Gomen... Maintenance is happening :<"))
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        jda.addEventListener(
                new FavoritesCommand(),
                new getRandomImage(),
                new ConfigCommand(),
                new ChannelNuke(),
                new InfoCommand()
        );

        jda.updateCommands().addCommands(
                Commands.slash("random", "Sends a random image")
                        .addSubcommands(
                                new SubcommandData("all", "Sends images from all sources")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("picsum", "Sends a random image from picsum")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("imgur", "Sends a random image from imgur")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("rule34xxx", "Sends a random image from rule34.xxx")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("google", "Sends a random image from google")
                                .addOption(OptionType.STRING, "query", "What image should be searched for?")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("reddit", "Sends a random image from reddit")
                                .addOption(OptionType.STRING, "subreddit", "Which subreddit should the image be retrieved from?")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("tenor", "Sends a random gif from tenor")
                                .addOption(OptionType.STRING, "query", "What gif should be searched for?")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the gif keep generating?"),
                                new SubcommandData("4chan", "Sends a random image from 4chan")
                                .addOption(OptionType.STRING, "query", "Which board to retrieve image from?")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("movie", "Sends a random movie from TMDB")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("tvshow", "Sends a random TV Show from TMDB")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("youtube", "Sends a random YouTube video")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("short", "Sends a random Short from TMDB")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the image keep generating?"),
                                new SubcommandData("urban", "Sends a random word from The Urban Dictionary")
                                .addOption(OptionType.STRING, "query", "What word should be defined?")
                                .addOption(OptionType.BOOLEAN, "shouldcontinue", 
                                        "Should the word keep generating?")         
                        ),

                Commands.slash("nuke", "Nukes and throws white fluids on old channel"),

                Commands.slash("favorites", "Shows your favorites"),

                Commands.slash("config", "Change personal, guild or bot settings")
                        .addSubcommands(
                                new SubcommandData("bot", "Change settings for the bot (Admin required)")
                                        .addOptions(new OptionData(OptionType.STRING, "option", "Field to change", true)
                                                .addChoice("Enable NSFW Webhook", "NSFW_WEBHOOK")
                                                .addChoice("Enable Safe Webhook", "SAFE_WEBHOOK")
                                                .addChoice("Enable Reddit", "REDDIT")
                                                .addChoice("Enable Google Search", "GOOGLE")
                                                .addChoice("Enable 4Chan", "4CHAN")
                                                .addChoice("Enable Picsum", "PICSUM")
                                                .addChoice("Enable Rule34.xxx", "RULE34XXX")
                                                .addChoice("Enable Tenor", "TENOR")
                                                .addChoice("Enable Generated Count", "GENERATED_VOICE_CHANNEL"))
                                        .addOption(OptionType.STRING, "value", "Value to set", false),
                                new SubcommandData("user", "Change settings for yourself")
                                        .addOptions(new OptionData(OptionType.STRING, "option", "Field to change", true)
                                                .addChoice("Set Chances for Images", "chances")
                                                .addChoice("Enable Image Option", "enable")
                                                .addChoice("Disable Image Option", "disable"))
                                        .addOption(OptionType.STRING, "value", "Value to set", true),
                                new SubcommandData("send", "Send your configuration to the current channel")
                                        .addOption(OptionType.STRING, "description", "Description for configuration", true),
                                new SubcommandData("reset_configuration", "Resets your configuration to default (Chances)"),
                                new SubcommandData("add", "Add something to the bot")
                                        .addOptions(new OptionData(OptionType.STRING, "option", "Something to add", true)
                                                .addChoice("Premium", "PREMIUM")
                                                .addChoice("Admin", "ADMIN"))
                                        .addOption(OptionType.USER, "value", "Whom to add to", true),
                                new SubcommandData("remove", "Add something to the bot")
                                        .addOptions(new OptionData(OptionType.STRING, "option", "Something to add", true)
                                                .addChoice("Premium", "PREMIUM")
                                                .addChoice("Admin", "ADMIN"))
                                        .addOption(OptionType.USER, "value", "Whom to add to", true)),
                
                Commands.slash("info", "Change personal, guild or bot settings")
                        .addSubcommands(
                                new SubcommandData("bot", "Get global info about hte bot"),
                                new SubcommandData("me", "Get info about yourself"))

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

                voiceChannel.getManager().setName("Generated: "
                        + Config.formatBigInteger(new BigInteger(imageGenerated))).queue();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public JDA getJDA() {
        return jda;
    }
}
