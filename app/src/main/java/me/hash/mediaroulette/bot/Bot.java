package me.hash.mediaroulette.bot;

import me.hash.mediaroulette.bot.commands.getRandomImage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class Bot {

    final JDA jda;

    public Bot(String token) {
        jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("The Media Roulette"))
                .build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        jda.addEventListener(new getRandomImage());

        jda.updateCommands().addCommands(
                Commands.slash("random", "Sends a random image")
                        .addOption(OptionType.BOOLEAN, "shouldContinue",
                                "Should the bot keep generating images after 1?", false))
                .queue();
    }

    public JDA getJDA() {
        return jda;
    }

}
