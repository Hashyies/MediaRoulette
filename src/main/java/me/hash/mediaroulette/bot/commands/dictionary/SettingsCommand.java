package me.hash.mediaroulette.bot.commands.dictionary;

import me.hash.mediaroulette.bot.Bot;
import me.hash.mediaroulette.bot.commands.CommandHandler;
import me.hash.mediaroulette.model.Dictionary;
import me.hash.mediaroulette.service.DictionaryService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SettingsCommand extends ListenerAdapter implements CommandHandler {
    private static final Color PRIMARY_COLOR = new Color(88, 101, 242);
    private static final Color SUCCESS_COLOR = new Color(87, 242, 135);
    private static final Color ERROR_COLOR = new Color(220, 53, 69);
    
    private static final List<String> SUPPORTED_SOURCES = Arrays.asList(
        "tenor", "google", "reddit"
    );
    
    private DictionaryService dictionaryService;
    
    public SettingsCommand(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash("settings", "⚙️ Configure dictionary assignments for sources")
                .addSubcommands(
                    new SubcommandData("assign", "Assign a dictionary to a source")
                        .addOption(OptionType.STRING, "source", "Source name (tenor, reddit, etc.)", true)
                        .addOption(OptionType.STRING, "dictionary", "Dictionary ID", true),
                    new SubcommandData("view", "View current assignments"),
                    new SubcommandData("unassign", "Remove dictionary assignment")
                        .addOption(OptionType.STRING, "source", "Source name", true)
                );
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("settings")) return;
        
        event.deferReply().queue();
        Bot.executor.execute(() -> {
            String subcommand = event.getSubcommandName();
            String userId = event.getUser().getId();
            
            switch (subcommand) {
                case "assign" -> handleAssign(event, userId);
                case "view" -> handleView(event, userId);
                case "unassign" -> handleUnassign(event, userId);
            }
        });
    }
    
    private void handleAssign(SlashCommandInteractionEvent event, String userId) {
        String source = event.getOption("source").getAsString().toLowerCase();
        String dictionaryId = event.getOption("dictionary").getAsString();
        
        if (!SUPPORTED_SOURCES.contains(source)) {
            sendError(event, "Unsupported source. Supported: " + String.join(", ", SUPPORTED_SOURCES));
            return;
        }
        
        Optional<Dictionary> dictOpt = dictionaryService.getDictionary(dictionaryId);
        if (dictOpt.isEmpty() || !dictOpt.get().canBeViewedBy(userId)) {
            sendError(event, "Dictionary not found or access denied.");
            return;
        }
        
        dictionaryService.assignDictionary(userId, source, dictionaryId);
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("✅ Assignment Complete")
            .setDescription(String.format("Dictionary **%s** assigned to **%s**", 
                dictOpt.get().getName(), source))
            .setColor(SUCCESS_COLOR);
            
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void handleView(SlashCommandInteractionEvent event, String userId) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("⚙️ Dictionary Assignments")
            .setColor(PRIMARY_COLOR);
            
        StringBuilder sb = new StringBuilder();
        boolean hasAssignments = false;
        
        for (String source : SUPPORTED_SOURCES) {
            Optional<String> assignedDict = dictionaryService.getAssignedDictionary(userId, source);
            if (assignedDict.isPresent()) {
                Optional<Dictionary> dict = dictionaryService.getDictionary(assignedDict.get());
                if (dict.isPresent()) {
                    sb.append(String.format("**%s**: %s (`%s`)\n", 
                        formatSourceName(source), dict.get().getName(), dict.get().getId()));
                    hasAssignments = true;
                }
            } else {
                sb.append(String.format("**%s**: *Default dictionary*\n", formatSourceName(source)));
            }
        }
        
        if (!hasAssignments) {
            embed.setDescription("No custom dictionary assignments. All sources use default dictionaries.");
        } else {
            embed.setDescription(sb.toString());
        }
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void handleUnassign(SlashCommandInteractionEvent event, String userId) {
        String source = event.getOption("source").getAsString().toLowerCase();
        
        if (!SUPPORTED_SOURCES.contains(source)) {
            sendError(event, "Unsupported source.");
            return;
        }
        
        // Remove the assignment
        if (dictionaryService.unassignDictionary(userId, source)) {
            sendSuccess(event, String.format("Dictionary assignment removed from **%s**. Now using default.", 
                formatSourceName(source)));
        } else {
            sendError(event, String.format("No dictionary assigned to **%s**.", formatSourceName(source)));
        }
    }
    
    private String formatSourceName(String source) {
        return switch (source) {
            case "tenor" -> "Tenor GIFs";
            case "reddit" -> "Reddit";
            case "google" -> "Google Images";
            default -> source.substring(0, 1).toUpperCase() + source.substring(1);
        };
    }
    
    private void sendSuccess(SlashCommandInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("✅ Success")
            .setDescription(message)
            .setColor(SUCCESS_COLOR);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void sendError(SlashCommandInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("❌ Error")
            .setDescription(message)
            .setColor(ERROR_COLOR);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}