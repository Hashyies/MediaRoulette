package me.hash.mediaroulette.utils.media.ffmpeg.processors;

import me.hash.mediaroulette.utils.media.ffmpeg.config.FFmpegConfig;
import me.hash.mediaroulette.utils.media.FFmpegDownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all FFmpeg processors
 */
public abstract class BaseProcessor {
    protected final FFmpegConfig config;

    protected BaseProcessor(FFmpegConfig config) {
        this.config = config;
    }

    /**
     * Executes an FFmpeg command with the specified timeout
     */
    protected CompletableFuture<ProcessResult> executeFFmpegCommand(List<String> command, int timeoutSeconds) {
        return FFmpegDownloader.getFFmpegPath().thenCompose(ffmpegPath -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    config.getFileManager().ensureTempDirectoryExists();

                    // Replace first element with actual ffmpeg path
                    command.set(0, ffmpegPath.toString());

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    StringBuilder output = new StringBuilder();
                    StringBuilder error = new StringBuilder();

                    // Read output in separate thread to prevent blocking
                    Thread outputReader = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                output.append(line).append("\n");
                            }
                        } catch (IOException e) {
                            error.append("Failed to read output: ").append(e.getMessage());
                        }
                    });
                    outputReader.start();

                    boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                    if (!finished) {
                        process.destroyForcibly();
                        outputReader.interrupt();
                        throw new RuntimeException("FFmpeg command timed out after " + timeoutSeconds + " seconds");
                    }

                    outputReader.join(1000); // Wait up to 1 second for output reader to finish

                    return new ProcessResult(process.exitValue(), output.toString(), error.toString());

                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute FFmpeg command: " + e.getMessage(), e);
                }
            });
        });
    }

    /**
     * Executes an FFprobe command with the specified timeout
     */
    protected CompletableFuture<ProcessResult> executeFFprobeCommand(List<String> command, int timeoutSeconds) {
        return FFmpegDownloader.getFFprobePath().thenCompose(ffprobePath -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Replace first element with actual ffprobe path
                    command.set(0, ffprobePath.toString());

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(false);
                    Process process = pb.start();

                    StringBuilder output = new StringBuilder();
                    StringBuilder error = new StringBuilder();

                    // Read both output and error streams
                    Thread outputReader = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                output.append(line).append("\n");
                            }
                        } catch (IOException e) {
                            // Ignore
                        }
                    });

                    Thread errorReader = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                error.append(line).append("\n");
                            }
                        } catch (IOException e) {
                            // Ignore
                        }
                    });

                    outputReader.start();
                    errorReader.start();

                    boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                    if (!finished) {
                        process.destroyForcibly();
                        outputReader.interrupt();
                        errorReader.interrupt();
                        throw new RuntimeException("FFprobe command timed out after " + timeoutSeconds + " seconds");
                    }

                    outputReader.join(1000);
                    errorReader.join(1000);

                    return new ProcessResult(process.exitValue(), output.toString(), error.toString());

                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute FFprobe command: " + e.getMessage(), e);
                }
            });
        });
    }

    /**
     * Result of a process execution
     */
    protected static class ProcessResult {
        private final int exitCode;
        private final String output;
        private final String error;

        public ProcessResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }

        public int getExitCode() { return exitCode; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public boolean isSuccessful() { return exitCode == 0; }
    }
}