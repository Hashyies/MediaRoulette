package me.hash.mediaroulette.utils.terminal.commands;

import me.hash.mediaroulette.utils.terminal.Command;
import me.hash.mediaroulette.utils.terminal.CommandResult;

import java.util.List;

public class ExitCommand extends Command {

    public ExitCommand() {
        super("exit", "Exit the application", "exit", List.of("quit", "q"));
    }

    @Override
    public CommandResult execute(String[] args) {
        System.out.println("Shutting down...");
        // Use the proper shutdown method instead of System.exit(0)
        me.hash.mediaroulette.Main.shutdown();
        return CommandResult.success("Goodbye!");
    }
}
