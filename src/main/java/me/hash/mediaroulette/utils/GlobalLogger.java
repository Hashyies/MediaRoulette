package me.hash.mediaroulette.utils;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GlobalLogger {

    private static final Logger logger = Logger.getLogger("GlobalLogger");

    static {
        try {
            // Set up a FileHandler to write logs to "app.log"
            FileHandler fileHandler = new FileHandler("app.log", true); // 'true' appends to the file
            fileHandler.setFormatter(new SimpleFormatter()); // Use a simple text formatter
            logger.addHandler(fileHandler); // Add the FileHandler to the logger

            // Set the default log level
            logger.setLevel(Level.ALL);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize the global logger", e);
        }
    }

    // Static method to get the global logger
    public static Logger getLogger() {
        return logger;
    }
}
