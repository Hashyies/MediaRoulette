package me.hash.mediaroulette.utils.terminal;

import me.hash.mediaroulette.utils.terminal.commands.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class TerminalInterface {
    private final CommandSystem commandSystem;
    private final BufferedReader reader;
    private volatile boolean running = true;

    public TerminalInterface() {
        this.commandSystem = new CommandSystem();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        registerCommands();
    }

    private void registerCommands() {
        commandSystem.registerCommand(new HelpCommand(commandSystem));
        commandSystem.registerCommand(new UserCommand());
        commandSystem.registerCommand(new ExitCommand());
        commandSystem.registerCommand(new StatusCommand());
    }

    public void start() {
        System.out.println("=== Media Roulette Terminal ===");
        System.out.println("Type 'help' for available commands or 'exit' to quit.");
        System.out.println();

        while (running) {
            try {
                System.out.print("mediaroulette> ");
                String input = reader.readLine();

                if (input == null) {
                    break; // EOF
                }

                input = input.trim();
                if (input.isEmpty()) {
                    continue;
                }

                // Handle tab completion simulation (basic)
                if (input.equals("tab")) {
                    showCompletions("");
                    continue;
                }

                CommandResult result = commandSystem.executeCommand(input);

                if (result.isSuccess()) {
                    if (!result.getMessage().isEmpty()) {
                        System.out.println(result.getMessage());
                    }
                } else {
                    System.err.println("Error: " + result.getMessage());
                }

                System.out.println();

            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showCompletions(String input) {
        List<String> completions = commandSystem.getCompletions(input);
        if (!completions.isEmpty()) {
            System.out.println("Available completions:");
            for (String completion : completions) {
                System.out.println("  " + completion);
            }
        } else {
            System.out.println("No completions available.");
        }
    }

    public void stop() {
        running = false;
    }
}