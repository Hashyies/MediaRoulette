package me.hash.mediaroulette.utils.terminal;

import java.util.List;

public abstract class Command {
    protected final String name;
    protected final String description;
    protected final String usage;
    protected final List<String> aliases;

    public Command(String name, String description, String usage, List<String> aliases) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.aliases = aliases != null ? aliases : List.of();
    }

    public abstract CommandResult execute(String[] args);

    public List<String> getCompletions(String[] args) {
        return List.of(); // Default: no completions
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getUsage() { return usage; }
    public List<String> getAliases() { return aliases; }
}
