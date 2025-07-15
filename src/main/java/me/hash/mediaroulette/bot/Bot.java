package me.hash.mediaroulette.bot;

import me.hash.mediaroulette.Main;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.bot.commands.admin.ChannelNuke;
import me.hash.mediaroulette.bot.commands.bot.InfoCommand;
import me.hash.mediaroulette.bot.commands.bot.ShardsCommand;
import me.hash.mediaroulette.bot.commands.bot.ThemeCommand;
import me.hash.mediaroulette.bot.commands.config.ConfigCommand;
import me.hash.mediaroulette.bot.commands.config.ChancesCommand;
import me.hash.mediaroulette.bot.commands.economy.BalanceCommand;
import me.hash.mediaroulette.bot.commands.economy.QuestsCommand;
import me.hash.mediaroulette.bot.commands.economy.ShopCommand;
import me.hash.mediaroulette.bot.commands.minigame.MediaHuntCommand;
import me.hash.mediaroulette.bot.commands.images.FavoritesCommand;
import me.hash.mediaroulette.bot.commands.images.getRandomImage;
import me.hash.mediaroulette.utils.Config;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.*;
import java.util.concurrent.*;

public class Bot {
    private static ShardManager shardManager = null;
    public static final long COOLDOWN_DURATION = 2500; // Cooldown duration in milliseconds
    public static final Map<Long, Long> COOLDOWNS = new HashMap<>(); // Cooldown management map
    public static Config config = null;
    public static final ExecutorService executor = Executors.newCachedThreadPool(); // Executor for async tasks

    public Bot(String token) {
        // Initialize ShardManager
        shardManager = DefaultShardManagerBuilder.createDefault(token)
                .setActivity(Activity.playing("ALPHAAAA WOOO! :3")) // Set activity to all shards
                .setStatus(OnlineStatus.ONLINE) // Default status
                .setShardsTotal(-1) // Auto-detect number of shards
                .build();

        // Register global event listeners and command handlers
        registerEventListeners();

        // Create and register global commands across all shards
        registerGlobalCommands();

        // Initialize configuration
        config = new Config(Main.database);

        // Add a shutdown hook to properly handle termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            if (shardManager != null) {
                shardManager.shutdown();
            }
        }));
    }

    /**
     * Registers all commands and event listeners with the ShardManager and its shards.
     */
    private void registerEventListeners() {
        if (shardManager != null) {
            List<CommandHandler> commandHandlers = Arrays.asList(
                    new FavoritesCommand(),
                    new getRandomImage(),
                    new ConfigCommand(),
                    new ChancesCommand(),
                    new ChannelNuke(),
                    new InfoCommand(),
                    new ShardsCommand(),
                    new ThemeCommand(),
                    new BalanceCommand(),
                    new QuestsCommand(),
                    new ShopCommand()
                    // new MediaHuntCommand() // Temporarily disabled
            );

            // Add all event listeners (global to the entire bot)
            commandHandlers.forEach(shardManager::addEventListener);

            System.out.println("Registered all event listeners.");
        }
    }

    /**
     * Registers global slash commands for all shards.
     */
    private void registerGlobalCommands() {
        if (shardManager != null) {
            // Collect all command data
            List<CommandData> commands = Arrays.asList(
                    new FavoritesCommand().getCommandData(),
                    new getRandomImage().getCommandData(),
                    new ConfigCommand().getCommandData(),
                    new ChancesCommand().getCommandData(),
                    new ChannelNuke().getCommandData(),
                    new InfoCommand().getCommandData(),
                    new ShardsCommand().getCommandData(),
                    new ThemeCommand().getCommandData(),
                    new BalanceCommand().getCommandData(),
                    new QuestsCommand().getCommandData(),
                    new ShopCommand().getCommandData()
                    // new MediaHuntCommand().getCommandData() // Temporarily disabled
            );

            shardManager.getShards().forEach(jda -> jda.updateCommands().addCommands(commands).queue());

            System.out.println("Registered all global slash commands.");
        }
    }

    /**
     * Public method to obtain the ShardManager instance.
     *
     * @return ShardManager instance.
     */
    public static ShardManager getShardManager() {
        return shardManager;
    }
}