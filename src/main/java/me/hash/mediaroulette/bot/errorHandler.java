package me.hash.mediaroulette.bot;

import io.github.cdimascio.dotenv.DotenvEntry;
import me.hash.mediaroulette.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class errorHandler {

    /**
     * Sends an error embed when an exception occurs.
     *
     * @param event       The interaction event (e.g., slash command or button interaction).
     * @param title       The title of the embed.
     * @param description The details about the error.
     */
    public static void sendErrorEmbed(Interaction event, String title, String description) {
        EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle(title != null && !title.isEmpty() ? title : "Error")
                .setDescription(description != null ? description : "An unexpected error occurred.")
                .setColor(Color.RED);

        switch (event) {
            case SlashCommandInteractionEvent slashCommandInteractionEvent -> slashCommandInteractionEvent.getHook()
                    .sendMessageEmbeds(errorEmbed.build())
                    .setEphemeral(true)
                    .queue();
            case ButtonInteractionEvent buttonInteractionEvent -> buttonInteractionEvent.getHook()
                    .sendMessageEmbeds(errorEmbed.build())
                    .setEphemeral(true)
                    .queue();
            case StringSelectInteraction stringSelectInteraction -> stringSelectInteraction.getHook()
                    .sendMessageEmbeds(errorEmbed.build())
                    .setEphemeral(true)
                    .queue();
            case null, default -> System.err.println("Unsupported interaction type for error embedding.");
        }
    }

    /**
     * Safely censors sensitive environment variables by masking their values, showing only
     * the first 3 characters followed by asterisks (***). If the value has fewer than 3 characters,
     * the entire value is replaced with asterisks.
     *
     * @return A map of environment variables with censored values.
     */
    public static Map<String, String> getCensoredEnvVariables() {
        Map<String, String> censoredEnv = new HashMap<>();

        // Loop through the environment variables in the application.
        for (DotenvEntry entry : Main.env.entries()) {
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                value = "***"; // If the value is empty, replace it entirely with asterisks.
            } else if (value.length() > 3) {
                value = value.substring(0, 3) + "***"; // Keep the first 3 characters, mask the rest.
            } else {
                value = "***"; // For values with fewer than 3 characters, mask them completely.
            }
            censoredEnv.put(entry.getKey(), value);
        }

        return censoredEnv;
    }

    /**
     * Handles exceptions globally by logging the throwable and sending an error embed to the user. It also
     * ensures that sensitive information like environment keys or values is censored from the stack trace.
     *
     * @param event   The interaction where the throwable occurred.
     * @param title   The title of the error embed.
     * @param message A custom user-friendly error message.
     * @param ex      The exception or throwable that was thrown.
     */
    public static void handleException(Interaction event, String title, String message, Throwable ex) {
        // Censor the stack trace to prevent sensitive information exposure.
        String censoredStackTrace = getCensoredStackTrace(ex);

        // Log the censored stack trace for debugging.
        System.err.println(censoredStackTrace);

        // Build the error message and send it to the user via embed.
        sendErrorEmbed(event, title, buildErrorDescription(message, ex));
    }

    /**
     * Constructs a detailed error message for embedding, combining a user-friendly message
     * with the exception's details.
     *
     * @param userMessage User-friendly error message.
     * @param throwable   The throwable/exception that occurred.
     * @return A detailed error message as a string.
     */
    private static String buildErrorDescription(String userMessage, Throwable throwable) {
        String throwableMessage = throwable.getMessage() != null ? throwable.getMessage() : "No additional details.";
        return userMessage + "\n\nDetails: " + throwableMessage;
    }

    /**
     * Censors sensitive data from a throwable's stack trace using the application's
     * environment variables.
     *
     * @param throwable The throwable whose stack trace needs to be censored.
     * @return A string containing the censored stack trace.
     */
    private static String getCensoredStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        // Write the stack trace to a StringWriter.
        throwable.printStackTrace(printWriter);
        String fullStackTrace = stringWriter.toString();

        // Get censored environment variables for replacement.
        Map<String, String> censoredEnvVariables = getCensoredEnvVariables();

        // Replace each environment variable's value in the stack trace with its censored counterpart.
        for (Map.Entry<String, String> entry : censoredEnvVariables.entrySet()) {
            String key = entry.getKey();
            String censoredValue = entry.getValue();

            // Replace both the key and possible raw values in the stack trace.
            fullStackTrace = fullStackTrace.replace(key, "***"); // Replace raw keys.
            fullStackTrace = fullStackTrace.replace(entry.getValue().replace("***", ""), censoredValue); // Replace raw uncensored values.
        }

        return fullStackTrace;
    }
}