package me.hash.mediaroulette.utils.terminal.commands;

import me.hash.mediaroulette.utils.terminal.Command;
import me.hash.mediaroulette.utils.terminal.CommandResult;
import me.hash.mediaroulette.utils.terminal.CommandSystem;

import java.util.List;

public class HelpCommand extends Command {
    private final CommandSystem commandSystem;

    public HelpCommand(CommandSystem commandSystem) {
        super("help", "Show help information", "help [command]", List.of("h", "?"));
        this.commandSystem = commandSystem;
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length == 0) {
            return CommandResult.success(commandSystem.getHelp());
        } else {
            String commandName = args[0].toLowerCase();
            // You could implement specific command help here
            return CommandResult.success("Help for command: " + commandName + " (not implemented yet)");
        }
    }

    @Override
    public List<String> getCompletions(String[] args) {
        if (args.length == 1) {
            return commandSystem.getCommandNames().stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .toList();
        }
        return List.of();
    }
}