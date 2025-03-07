package me.hash.mediaroulette.content;

import me.hash.mediaroulette.utils.discord.DiscordTimestamp;
import me.hash.mediaroulette.utils.discord.DiscordTimestampType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RandomText {

    private static final String BASE_URL = "https://api.urbandictionary.com/v0";
    private static final OkHttpClient client = new OkHttpClient();

    public static Map<String, String> getRandomUrbanWord(String meaning) throws IOException {
        Request request;
        if (meaning != null) {
            request = new Request.Builder()
                    .url(BASE_URL + "/define?term=" + meaning)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(BASE_URL + "/random")
                    .build();
        }

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);

            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONArray list = jsonObject.getJSONArray("list");
            if (list.isEmpty()) {
                return Map.of("error", "No word found with the given meaning.");
            }

            JSONObject randomWord;
            if (meaning != null) {
                randomWord = list.getJSONObject(0);
            } else {
                randomWord = list.getJSONObject(new Random().nextInt(list.length()));
            }

            // Parse values from the JSON response
            String word = randomWord.getString("word");
            String definition = randomWord.getString("definition");
            String author = randomWord.getString("author");
            String writtenOn = randomWord.getString("written_on");

            // Replace any [text] with clickable links using Pattern and Matcher
            String processedDefinition = processDefinition(definition);

            // Construct the description with emojis and clickable links
            String description = "📖 **Word:** " + word
                    + "\n📝 **Meaning:** " + processedDefinition
                    + "\n🖋️ **Submitted by:** [" + author + "](https://www.urbandictionary.com/author.php?name=" + author.replace(" ", "%20") + ")"
                    + "\n📅 **Date:** " + DiscordTimestamp.generateTimestampFromIso8601(writtenOn, DiscordTimestampType.SHORT_DATE_TIME);

            // Prepare the map to return
            Map<String, String> map = new HashMap<>();
            map.put("description", description);
            map.put("image", "attachment://image.png");
            map.put("image_type", "create");
            map.put("image_content", word);
            map.put("title", "🎲 Here's your random Urban Dictionary word!");
            return map;
        }
    }

    /**
     * Processes the definition string, replacing occurrences of [word]
     * with clickable links of the format [word](<a href="https://www.urbandictionary.com/define.php?term=word">...</a>).
     */
    private static String processDefinition(String definition) {
        // Regex pattern to find words/phrases in square brackets
        Pattern pattern = Pattern.compile("\\[(.+?)]");
        Matcher matcher = pattern.matcher(definition);

        StringBuilder processedDefinition = new StringBuilder();
        while (matcher.find()) {
            // Extract the term inside square brackets
            String term = matcher.group(1);
            // URL-encode the term to replace spaces and other special characters
            String encodedTerm = term.replace(" ", "%20");
            // Replace [word] with [word](URL)
            String replacement = "[" + term + "](https://www.urbandictionary.com/define.php?term=" + encodedTerm + ")";
            matcher.appendReplacement(processedDefinition, replacement);
        }
        matcher.appendTail(processedDefinition);

        return processedDefinition.toString();
    }

}