package me.hash.mediaroulette.utils.terminal.commands;

import me.hash.mediaroulette.utils.RateLimiter;
import me.hash.mediaroulette.utils.terminal.Command;
import me.hash.mediaroulette.utils.terminal.CommandResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitCommand extends Command {

    public RateLimitCommand() {
        super("ratelimit", "Manage rate limits for API sources", "ratelimit <action> [source] [duration]", List.of("rl"));
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length < 1) {
            return CommandResult.error("Usage: " + getUsage() + "\nActions: status, reset, trigger");
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "status":
                return showRateLimitStatus();
            
            case "reset":
                if (args.length < 2) {
                    return CommandResult.error("Usage: ratelimit reset <source>");
                }
                return resetRateLimit(args[1]);
            
            case "trigger":
                if (args.length < 3) {
                    return CommandResult.error("Usage: ratelimit trigger <source> <duration_seconds>");
                }
                try {
                    int duration = Integer.parseInt(args[2]);
                    return triggerRateLimit(args[1], duration);
                } catch (NumberFormatException e) {
                    return CommandResult.error("Invalid duration: " + args[2]);
                }
            
            default:
                return CommandResult.error("Unknown action: " + action + "\nAvailable actions: status, reset, trigger");
        }
    }

    @Override
    public List<String> getCompletions(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Auto-complete actions
            String partial = args[0].toLowerCase();
            List<String> actions = List.of("status", "reset", "trigger");
            for (String action : actions) {
                if (action.startsWith(partial)) {
                    completions.add(action);
                }
            }
        } else if (args.length == 2 && ("reset".equalsIgnoreCase(args[0]) || "trigger".equalsIgnoreCase(args[0]))) {
            // Auto-complete source names
            String partial = args[1].toLowerCase();
            List<String> sources = List.of("reddit", "4chan", "tenor", "google", "youtube", "tmdb", "imgur", "rule34", "picsum", "urban");
            for (String source : sources) {
                if (source.startsWith(partial)) {
                    completions.add(source);
                }
            }
        } else if (args.length == 3 && "trigger".equalsIgnoreCase(args[0])) {
            // Auto-complete duration suggestions
            completions.addAll(List.of("60", "300", "600", "1800", "3600"));
        }

        return completions;
    }

    private CommandResult showRateLimitStatus() {
        try {
            ConcurrentHashMap<String, String> status = RateLimiter.getAllRateLimitStatus();
            
            StringBuilder result = new StringBuilder();
            result.append("=== RATE LIMIT STATUS ===\n");
            
            for (String source : status.keySet()) {
                String statusText = status.get(source);
                int limit = RateLimiter.getRateLimit(source);
                
                result.append(String.format("%-10s: %s (Limit: %d/min)\n", 
                    source.toUpperCase(), statusText, limit));
            }
            
            result.append("=========================\n");
            result.append("Legend: OK = Normal operation, RATE LIMITED = Currently limited");
            
            return CommandResult.success(result.toString());
        } catch (Exception e) {
            return CommandResult.error("Failed to get rate limit status: " + e.getMessage());
        }
    }

    private CommandResult resetRateLimit(String source) {
        try {
            RateLimiter.resetRateLimit(source);
            return CommandResult.success("Rate limit reset for source: " + source.toUpperCase());
        } catch (Exception e) {
            return CommandResult.error("Failed to reset rate limit for " + source + ": " + e.getMessage());
        }
    }

    private CommandResult triggerRateLimit(String source, int durationSeconds) {
        try {
            RateLimiter.triggerRateLimit(source, durationSeconds);
            return CommandResult.success(String.format("Manual rate limit triggered for %s for %d seconds", 
                source.toUpperCase(), durationSeconds));
        } catch (Exception e) {
            return CommandResult.error("Failed to trigger rate limit for " + source + ": " + e.getMessage());
        }
    }
}