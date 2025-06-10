package me.hash.mediaroulette;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class RandomDictionaryLineFetcher {
    private static final int CHUNK_SIZE = 4096;
    private final OkHttpClient client;
    private final String dictionaryUrl;
    private final Random random;

    public RandomDictionaryLineFetcher(String dictionaryUrl) {
        this.client = new OkHttpClient();
        this.dictionaryUrl = dictionaryUrl;
        this.random = new Random();
    }

    public String getRandomLine() throws IOException {
        Request headRequest = new Request.Builder().url(dictionaryUrl).head().build();

        try (Response headResponse = client.newCall(headRequest).execute()) {
            if (!headResponse.isSuccessful()) throw new IOException("HEAD request failed.");

            String contentLengthStr = headResponse.header("Content-Length");
            if (contentLengthStr == null) throw new IOException("Missing Content-Length header.");

            long contentLength = Long.parseLong(contentLengthStr);
            if (contentLength <= 0) throw new IOException("Invalid content length.");

            long randomOffset = (long) (random.nextDouble() * contentLength);
            long start = Math.max(0, randomOffset - CHUNK_SIZE / 2);
            long end = Math.min(contentLength - 1, start + CHUNK_SIZE - 1);

            Request getRequest = new Request.Builder()
                    .url(dictionaryUrl)
                    .header("Range", "bytes=" + start + "-" + end)
                    .build();

            try (Response getResponse = client.newCall(getRequest).execute()) {
                if (!getResponse.isSuccessful() || getResponse.body() == null)
                    throw new IOException("GET request failed.");

                InputStream inputStream = getResponse.body().byteStream();
                byte[] bytes = inputStream.readAllBytes();
                String chunk = new String(bytes, StandardCharsets.UTF_8);

                int relativeOffset = (int) (randomOffset - start);
                relativeOffset = Math.max(0, Math.min(relativeOffset, chunk.length() - 1));

                int lineStart = chunk.lastIndexOf('\n', relativeOffset);
                int lineEnd = chunk.indexOf('\n', relativeOffset);

                if (lineStart == -1) lineStart = 0;
                if (lineEnd == -1) lineEnd = chunk.length();
                if (lineStart >= lineEnd) return getRandomLine();

                return chunk.substring(lineStart + 1, lineEnd).trim();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String dictionaryUrl = "https://raw.githubusercontent.com/dustyfresh/dictionaries/refs/heads/master/DirBuster-Lists/directory-list-2.3-big.txt";
        RandomDictionaryLineFetcher fetcher = new RandomDictionaryLineFetcher(dictionaryUrl);


        try {
            String randomLine = fetcher.getRandomLine();
            System.out.println("Random line from dictionary: " + randomLine);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
