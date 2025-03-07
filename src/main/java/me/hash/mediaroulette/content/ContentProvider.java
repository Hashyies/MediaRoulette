package me.hash.mediaroulette.content;

public interface ContentProvider {
    /**
     * Fetch a random piece of content.
     *
     * @return a ContentInfo object containing details about the content.
     * @throws Exception if the content could not be fetched.
     */
    ContentInfo getRandomContent() throws Exception;
}
