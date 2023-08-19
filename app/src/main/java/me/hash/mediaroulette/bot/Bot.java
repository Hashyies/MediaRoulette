package me.hash.mediaroulette.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

public class Bot {

    final JDA jda;
    
    public Bot(String token) {
        jda = JDABuilder.createDefault(token)
            .setActivity(Activity.playing("The Media Roulette"))
            .build();
    }

    public JDA getJDA() {
        return jda;
    }

}
