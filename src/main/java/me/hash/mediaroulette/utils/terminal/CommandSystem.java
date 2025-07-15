package me.hash.mediaroulette.utils.terminal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandSystem {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    public void registerCommand(Command command) {
        commands.put(command.getName().toLowerCase(), command);
        for (String alias : command.getAliases()) {
            aliases.put(alias.toLowerCase(), command.getName().toLowerCase());
        }
    }

    public CommandResult executeCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new CommandResult(false, "Empty command");
        }

        String[] parts = input.trim().split("\\s+");
        String commandName = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        // Check aliases first
        if (aliases.containsKey(commandName)) {
            commandName = aliases.get(commandName);
        }

        Command command = commands.get(commandName);
        if (command == null) {
            return new CommandResult(false, "Unknown command: " + parts[0]);
        }

        try {
            return command.execute(args);
        } catch (Exception e) {
            return new CommandResult(false, "Error executing command: " + e.getMessage());
        }
    }

    public List<String> getCompletions(String input) {
        List<String> completions = new ArrayList<>();

        if (input == null || input.trim().isEmpty()) {
            // Return all command names
            completions.addAll(commands.keySet());
            completions.addAll(aliases.keySet());
            return completions;
        }

        String[] parts = input.trim().split("\\s+");

        if (parts.length == 1) {
            // Complete command name
            String partial = parts[0].toLowerCase();
            for (String cmdName : commands.keySet()) {
                if (cmdName.startsWith(partial)) {
                    completions.add(cmdName);
                }
            }
            for (String alias : aliases.keySet()) {
                if (alias.startsWith(partial)) {
                    completions.add(alias);
                }
            }
        } else {
            // Complete command arguments
            String commandName = parts[0].toLowerCase();
            if (aliases.containsKey(commandName)) {
                commandName = aliases.get(commandName);
            }

            Command command = commands.get(commandName);
            if (command != null) {
                String[] args = Arrays.copyOfRange(parts, 1, parts.length);
                completions.addAll(command.getCompletions(args));
            }
        }

        Collections.sort(completions);
        return completions;
    }

    public String getHelp() {
        StringBuilder help = new StringBuilder();
        help.append("Available commands:\n");

        for (Command command : commands.values()) {
            help.append("  ").append(command.getName())
                    .append(" - ").append(command.getDescription()).append("\n");
            help.append("    Usage: ").append(command.getUsage()).append("\n");
            if (!command.getAliases().isEmpty()) {
                help.append("    Aliases: ").append(String.join(", ", command.getAliases())).append("\n");
            }
            help.append("\n");
        }

        return help.toString();
    }

    public Set<String> getCommandNames() {
        return new HashSet<>(commands.keySet());
    }
}