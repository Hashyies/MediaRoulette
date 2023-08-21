package me.hash.mediaroulette.bot;

import java.util.HashMap;
import java.util.Map;

import me.hash.mediaroulette.bot.commands.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class Bot {

    final JDA jda;
    public static final long COOLDOWN_DURATION = 2500; // 2.5 seconds in milliseconds
    public static final Map<Long, Long> COOLDOWNS = new HashMap<>();

    public Bot(String token) {
        jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("The Media Roulette"))
                .build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        jda.addEventListener(new getRandomImage(), new randomQuery());

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
                Commands.slash("random-rule34xxx", "Sends a random image")
        ).queue();
    }

    public JDA getJDA() {
        return jda;
    }

}