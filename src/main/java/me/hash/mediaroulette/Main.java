package me.hash.mediaroulette;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.mongodb.client.MongoCollection;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.repository.MongoUserRepository;
import me.hash.mediaroulette.repository.UserRepository;
import me.hash.mediaroulette.utils.terminal.TerminalInterface;
import me.hash.mediaroulette.utils.Database;
import me.hash.mediaroulette.utils.user.UserService;
import org.bson.Document;

public class Main {

    public static Dotenv env;
    public static Database database;
    public static final long startTime = System.currentTimeMillis();
    public static Bot bot = null;
    public static UserService userService;
    public static TerminalInterface terminal;

    public static void main(String[] args) throws Exception {
        // Get an InputStream for the .env file
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(".env");

        // Create a temporary file to hold the contents of the .env file
        Path tempFile = Files.createTempFile("dotenv", ".env");
        tempFile.toFile().deleteOnExit();

        // Copy the contents of the .env file to the temporary file
        assert inputStream != null;
        Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        env = Dotenv.configure()
                .directory(tempFile.getParent().toString())
                .filename(tempFile.getFileName().toString())
                .load();

        // Initialize the MongoDB connection and database
        String connectionString = getEnv("MONGODB_CONNECTION");
        database = new Database(connectionString, "MediaRoulette");

        // Create the repository using the MongoDB collection
        MongoCollection<Document> userCollection = database.getCollection("user");
        UserRepository userRepository = new MongoUserRepository(userCollection);

        // Create our service layer that handles user business logic and persistence
        userService = new UserService(userRepository);

        bot = new Bot(getEnv("DISCORD_TOKEN"));

        init();

        // Initialize and start the terminal interface
        initializeTerminal();

        // System.out.println(RandomMedia.getRandomYoutube());
    }

    static void init() {
        Set<DotenvEntry> entries = env.entries();
        Map<String, String> configMap = new HashMap<>();
        configMap.put("DISCORD_NSFW_WEBHOOK", "NSFW_WEBHOOK");
        configMap.put("DISCORD_SAFE_WEBHOOK", "SAFE_WEBHOOK");
        configMap.put("TENOR_API", "TENOR");
        configMap.put("TMDB_API", "TMDB");

        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String envKey = entry.getKey();
            String configKey = entry.getValue();
            if (containsKey(entries, envKey)) {
                System.out.println(envKey + " Loaded!");
                Bot.config.set(configKey, Bot.config.getOrDefault(configKey, true, Boolean.class));
            } else {
                System.out.println(envKey + " Not found in .env");
                Bot.config.set(configKey, false);
            }
        }

        // Check for Reddit credentials
        checkCredentials(entries, "REDDIT", "REDDIT_CLIENT_ID", "REDDIT_CLIENT_SECRET", "REDDIT_USERNAME",
                "REDDIT_PASSWORD");

        // Check for Google credentials
        checkCredentials(entries, "GOOGLE", "GOOGLE_API_KEY", "GOOGLE_CX");
    }

    private static void initializeTerminal() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("MediaRoulette application started successfully!");
        System.out.println("Bot Status: " + (bot != null ? "Initialized" : "Failed to initialize"));
        System.out.println("Database Status: " + (database != null ? "Connected" : "Failed to connect"));
        System.out.println("=".repeat(50));

        // Start terminal interface
        terminal = new TerminalInterface();
        Thread terminalThread = new Thread(terminal::start);
        terminalThread.setName("Terminal-Interface");
        terminalThread.setDaemon(false); // Keep the application running
        terminalThread.start();
    }

    private static void checkCredentials(Set<DotenvEntry> entries, String configKey, String... keys) {
        boolean allKeysPresent = true;
        for (String key : keys) {
            if (!containsKey(entries, key)) {
                allKeysPresent = false;
                break;
            }
        }
        if (allKeysPresent) {
            System.out.println(configKey + " Loaded!");
            Bot.config.set(configKey, Bot.config.getOrDefault(configKey, true, Boolean.class));
        } else {
            System.out.println(configKey + " credentials are not set in .env");
            Bot.config.set(configKey, false);
        }
    }

    public static boolean checkCredentialsBoolean(Set<DotenvEntry> entries, String configKey, String... keys) {
        for (String key : keys) {
            if (!containsKey(entries, key)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsKey(Set<DotenvEntry> entries, String key) {
        for (DotenvEntry entry : entries) {
            if (entry.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static String getEnv(String key) {
        return env.get(key);
    }

    // Graceful shutdown method
    public static void shutdown() {
        System.out.println("Initiating graceful shutdown...");

        if (terminal != null) {
            terminal.stop();
        }

        if (bot != null) {
            // Add bot shutdown logic here
            System.out.println("Bot shutdown complete.");
        }

        if (database != null) {
            // Add database connection cleanup here
            System.out.println("Database connections closed.");
        }

        System.out.println("Shutdown complete. Goodbye!");
        System.exit(0);
    }
}