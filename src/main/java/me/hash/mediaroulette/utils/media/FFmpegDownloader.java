package me.hash.mediaroulette.utils.media;

import okhttp3.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for downloading and managing FFmpeg binaries across different operating systems.
 * Automatically detects the system architecture and downloads the appropriate FFmpeg version.
 */
public class FFmpegDownloader {
    private static final String FFMPEG_DIR = getJarDirectory() + File.separator + "ffmpeg";
    private static final String FFMPEG_EXECUTABLE_NAME = getExecutableName();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    
    // FFmpeg download URLs for different platforms and architectures
    private static final String WINDOWS_X64_URL    = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl-shared.zip";
    private static final String WINDOWS_ARM64_URL  = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-winarm64-gpl-shared.zip";
    private static final String LINUX_X64_URL      = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linux64-gpl.tar.xz";
    private static final String LINUX_ARM64_URL    = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linuxarm64-gpl.tar.xz";
    private static final String MACOS_X64_URL      = "https://evermeet.cx/ffmpeg/getrelease/zip";
    private static final String MACOS_ARM64_URL    = "https://evermeet.cx/ffmpeg/getrelease/arm64/zip";
    
    private static Path ffmpegPath;
    private static boolean isDownloaded = false;
    
    /**
     * Gets the path to the FFmpeg executable, downloading it if necessary
     */
    public static CompletableFuture<Path> getFFmpegPath() {
        if (isDownloaded && ffmpegPath != null && Files.exists(ffmpegPath)) {
            return CompletableFuture.completedFuture(ffmpegPath);
        }
        
        return downloadFFmpeg().thenApply(path -> {
            ffmpegPath = path;
            isDownloaded = true;
            return path;
        });
    }
    
    /**
     * Gets the path to the FFprobe executable
     */
    public static CompletableFuture<Path> getFFprobePath() {
        return getFFmpegPath().thenApply(ffmpegPath -> {
            Path ffmpegDir = ffmpegPath.getParent();
            String ffprobeExecutable = System.getProperty("os.name").toLowerCase().contains("windows") ? "ffprobe.exe" : "ffprobe";
            return ffmpegDir.resolve(ffprobeExecutable);
        });
    }
    
    /**
     * Checks if FFprobe is available
     */
    public static CompletableFuture<Boolean> isFFprobeAvailable() {
        return getFFprobePath().thenApply(Files::exists);
    }
    
    /**
     * Downloads FFmpeg for the current operating system
     */
    public static CompletableFuture<Path> downloadFFmpeg() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Detecting system information...");
                SystemInfo systemInfo = detectSystem();
                System.out.println("Detected system: " + systemInfo.os + " " + systemInfo.arch);
                
                // Create ffmpeg directory if it doesn't exist
                Path ffmpegDir = Paths.get(FFMPEG_DIR);
                Files.createDirectories(ffmpegDir);
                System.out.println("FFmpeg directory: " + ffmpegDir.toAbsolutePath());
                
                // Check if FFmpeg already exists
                Path existingPath = findExistingFFmpeg(ffmpegDir);
                if (existingPath != null) {
                    System.out.println("FFmpeg already exists at: " + existingPath);
                    return existingPath;
                }
                
                String downloadUrl = getDownloadUrl(systemInfo);
                System.out.println("Downloading FFmpeg from: " + downloadUrl);
                
                // Download the archive
                Path downloadedFile = downloadFile(downloadUrl, ffmpegDir);
                System.out.println("Downloaded to: " + downloadedFile);
                
                // Extract the archive
                Path extractedPath = extractArchive(downloadedFile, ffmpegDir, systemInfo);
                System.out.println("Extracted FFmpeg to: " + extractedPath);
                
                // Clean up downloaded archive
                Files.deleteIfExists(downloadedFile);
                
                // Make executable on Unix systems
                if (systemInfo.os != OperatingSystem.WINDOWS) {
                    makeExecutable(extractedPath);
                    
                    // Also make ffprobe executable if it exists
                    Path ffprobePath = extractedPath.getParent().resolve(getProbeExecutableName());
                    if (Files.exists(ffprobePath)) {
                        makeExecutable(ffprobePath);
                    }
                }
                
                return extractedPath;
                
            } catch (IOException e) {
                throw new RuntimeException("Failed to download FFmpeg: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException("Failed to download FFmpeg: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Checks if FFmpeg is available (either downloaded or in system PATH)
     */
    public static boolean isFFmpegAvailable() {
        // Check if we have a downloaded version
        if (isDownloaded && ffmpegPath != null && Files.exists(ffmpegPath)) {
            return true;
        }
        
        // Check if FFmpeg is in system PATH
        try {
            ProcessBuilder pb = new ProcessBuilder(FFMPEG_EXECUTABLE_NAME, "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the FFmpeg executable name for the current OS
     */
    private static String getExecutableName() {
        return System.getProperty("os.name").toLowerCase().contains("windows") ? "ffmpeg.exe" : "ffmpeg";
    }
    
    /**
     * Gets the directory where the JAR file is located
     */
    private static String getJarDirectory() {
        try {
            URI jarUri = FFmpegDownloader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            
            if (jarUri.getScheme().equals("file")) {
                File jarFile = new File(jarUri);
                String parentDir = jarFile.getParent();
                if (parentDir != null) {
                    System.out.println("JAR directory detected: " + parentDir);
                    return parentDir;
                }
            }
        } catch (Exception e) {
            System.err.println("Could not determine JAR directory: " + e.getMessage());
        }
        
        // Fallback to current working directory
        String currentDir = System.getProperty("user.dir");
        System.out.println("Using current directory: " + currentDir);
        return currentDir;
    }
    
    /**
     * Detects the current operating system and architecture
     */
    private static SystemInfo detectSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        
        OperatingSystem os;
        Architecture arch;
        
        // Detect OS
        if (osName.contains("windows")) {
            os = OperatingSystem.WINDOWS;
        } else if (osName.contains("linux")) {
            os = OperatingSystem.LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            os = OperatingSystem.MACOS;
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }
        
        // Detect Architecture
        if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            arch = Architecture.ARM64;
        } else if (osArch.contains("x86_64") || osArch.contains("amd64")) {
            arch = Architecture.X64;
        } else if (osArch.contains("arm")) {
            arch = Architecture.ARM64; // Assume ARM64 for ARM variants
        } else {
            System.out.println("Unknown architecture: " + osArch + ", defaulting to x64");
            arch = Architecture.X64;
        }
        
        return new SystemInfo(os, arch);
    }
    
    /**
     * Gets the download URL for the specified system
     */
    private static String getDownloadUrl(SystemInfo systemInfo) {
        return switch (systemInfo.os) {
            case WINDOWS -> systemInfo.arch == Architecture.ARM64 ? WINDOWS_ARM64_URL : WINDOWS_X64_URL;
            case LINUX -> systemInfo.arch == Architecture.ARM64 ? LINUX_ARM64_URL : LINUX_X64_URL;
            case MACOS -> systemInfo.arch == Architecture.ARM64 ? MACOS_ARM64_URL : MACOS_X64_URL;
        };
    }
    
    /**
     * Downloads a file from the given URL with progress tracking
     */
    private static Path downloadFile(String url, Path targetDir) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "MediaRoulette-Bot/1.0")
                .build();
        
        String fileName = getFileNameFromUrl(url);
        Path targetFile = targetDir.resolve(fileName);
        
        System.out.println("Requesting: " + url);
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            System.out.println("Response: HTTP " + response.code() + " " + response.message());
            if (response.request().url().toString().equals(url)) {
                System.out.println("Direct download (no redirect)");
            } else {
                System.out.println("Redirected to: " + response.request().url());
            }
            
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download file: HTTP " + response.code() + " " + response.message() + 
                                    "\nFinal URL: " + response.request().url());
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body is null");
            }
            
            long contentLength = body.contentLength();
            
            try (InputStream inputStream = body.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(targetFile.toFile())) {
                
                byte[] buffer = new byte[8192];
                long totalBytes = 0;
                int bytesRead;
                long lastProgressUpdate = 0;
                
                System.out.println("Downloading FFmpeg... (Size: " + formatBytes(contentLength) + ")");
                printProgressBar(0, contentLength);
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                    
                    // Update progress bar every 100KB or when complete
                    if (totalBytes - lastProgressUpdate >= 102400 || totalBytes == contentLength) {
                        printProgressBar(totalBytes, contentLength);
                        lastProgressUpdate = totalBytes;
                    }
                }
                
                System.out.println("\n✅ Download complete: " + formatBytes(totalBytes));
            }
        }
        
        return targetFile;
    }
    
    /**
     * Prints a progress bar for download progress
     */
    private static void printProgressBar(long downloaded, long total) {
        if (total <= 0) {
            System.out.print("\rDownloading... " + formatBytes(downloaded));
            return;
        }
        
        int barLength = 40;
        double progress = (double) downloaded / total;
        int filledLength = (int) (barLength * progress);
        
        StringBuilder bar = new StringBuilder();
        bar.append("\r[");
        
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        
        bar.append("] ");
        bar.append(String.format("%.1f%%", progress * 100));
        bar.append(" (").append(formatBytes(downloaded));
        if (total > 0) {
            bar.append("/").append(formatBytes(total));
        }
        bar.append(")");
        
        System.out.print(bar.toString());
    }
    
    /**
     * Formats bytes into human-readable format
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Extracts the downloaded archive and finds the FFmpeg executable
     */
    private static Path extractArchive(Path archiveFile, Path targetDir, SystemInfo systemInfo) throws IOException {
        String fileName = archiveFile.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".zip")) {
            return extractZip(archiveFile, targetDir);
        } else if (fileName.endsWith(".tar.xz")) {
            return extractTarXz(archiveFile, targetDir);
        } else {
            throw new UnsupportedOperationException("Unsupported archive format: " + fileName);
        }
    }
    
    /**
     * Extracts a ZIP archive and finds the FFmpeg executable
     */
    private static Path extractZip(Path zipFile, Path targetDir) throws IOException {
        System.out.println("Extracting ZIP archive: " + zipFile.getFileName());
        
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            Path ffmpegPath = null;
            int extractedFiles = 0;
            
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                
                String entryName = entry.getName();
                String fileName = Paths.get(entryName).getFileName().toString();
                
                // Extract FFmpeg executable
                if (fileName.equals(FFMPEG_EXECUTABLE_NAME)) {
                    ffmpegPath = targetDir.resolve(FFMPEG_EXECUTABLE_NAME);
                    extractFile(zis, ffmpegPath);
                    extractedFiles++;
                    System.out.println("✅ Extracted: " + fileName);
                }
                // Also extract ffprobe if available
                else if (fileName.equals(getProbeExecutableName())) {
                    Path ffprobePath = targetDir.resolve(getProbeExecutableName());
                    extractFile(zis, ffprobePath);
                    extractedFiles++;
                    System.out.println("✅ Extracted: " + fileName);
                }
                // Extract any DLL files on Windows
                else if (fileName.endsWith(".dll") && System.getProperty("os.name").toLowerCase().contains("windows")) {
                    Path dllPath = targetDir.resolve(fileName);
                    extractFile(zis, dllPath);
                    extractedFiles++;
                    System.out.println("✅ Extracted: " + fileName);
                }
                
                zis.closeEntry();
            }
            
            System.out.println("Extracted " + extractedFiles + " files from ZIP archive");
            
            if (ffmpegPath == null) {
                throw new IOException("FFmpeg executable not found in ZIP archive");
            }
            
            return ffmpegPath;
        }
    }
    
    /**
     * Extracts a single file from ZIP stream
     */
    private static void extractFile(ZipInputStream zis, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = zis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }
    
    /**
     * Gets the FFprobe executable name for the current OS
     */
    private static String getProbeExecutableName() {
        return System.getProperty("os.name").toLowerCase().contains("windows") ? "ffprobe.exe" : "ffprobe";
    }
    
    /**
     * Extracts a TAR.XZ archive (Linux) - Enhanced version
     */
    private static Path extractTarXz(Path tarXzFile, Path targetDir) throws IOException {
        System.out.println("Extracting TAR.XZ archive: " + tarXzFile.getFileName());
        
        // Try multiple extraction methods
        Path ffmpegPath = null;
        
        // Method 1: Try system tar command
        try {
            System.out.println("Attempting extraction with system tar command...");
            ProcessBuilder pb = new ProcessBuilder("tar", "-xf", tarXzFile.toString(), "-C", targetDir.toString());
            Process process = pb.start();
            
            // Capture output for debugging
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("tar: " + line);
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("✅ TAR extraction successful");
                ffmpegPath = findFFmpegInDirectory(targetDir);
                if (ffmpegPath != null) {
                    System.out.println("✅ Found FFmpeg at: " + ffmpegPath);
                    return ffmpegPath;
                }
            } else {
                System.err.println("tar command failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("System tar extraction failed: " + e.getMessage());
        }
        
        // Method 2: Try with different tar options
        try {
            System.out.println("Attempting extraction with alternative tar options...");
            ProcessBuilder pb = new ProcessBuilder("tar", "-xJf", tarXzFile.toString(), "-C", targetDir.toString());
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                ffmpegPath = findFFmpegInDirectory(targetDir);
                if (ffmpegPath != null) {
                    return ffmpegPath;
                }
            }
        } catch (Exception e) {
            System.err.println("Alternative tar extraction failed: " + e.getMessage());
        }
        
        // Method 3: Manual extraction instructions
        System.err.println("Automatic extraction failed. Manual extraction required:");
        System.err.println("1. Extract " + tarXzFile + " to " + targetDir);
        System.err.println("2. Ensure ffmpeg executable is in " + targetDir);
        System.err.println("3. Make sure ffmpeg has execute permissions (chmod +x ffmpeg)");
        
        throw new IOException("Failed to extract TAR.XZ archive automatically. " +
                            "Please extract manually or install tar/xz-utils.");
    }
    
    /**
     * Finds an existing FFmpeg executable in the given directory
     */
    private static Path findExistingFFmpeg(Path directory) {
        Path directPath = directory.resolve(FFMPEG_EXECUTABLE_NAME);
        if (Files.exists(directPath)) {
            return directPath;
        }
        
        return findFFmpegInDirectory(directory);
    }
    
    /**
     * Recursively searches for FFmpeg executable in directory
     */
    private static Path findFFmpegInDirectory(Path directory) {
        try {
            return Files.walk(directory)
                    .filter(path -> path.getFileName().toString().equals(FFMPEG_EXECUTABLE_NAME))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Makes a file executable on Unix systems
     */
    private static void makeExecutable(Path file) throws IOException {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                ProcessBuilder pb = new ProcessBuilder("chmod", "+x", file.toString());
                Process process = pb.start();
                process.waitFor();
            } catch (Exception e) {
                System.err.println("Failed to make FFmpeg executable: " + e.getMessage());
            }
        }
    }
    
    /**
     * Extracts filename from URL
     */
    private static String getFileNameFromUrl(String url) {
        if (url.contains("evermeet.cx")) {
            return "ffmpeg-macos.zip";
        }
        
        String[] parts = url.split("/");
        String fileName = parts[parts.length - 1];
        
        // Handle query parameters
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }
        
        return fileName;
    }
    
    /**
     * Operating system enumeration
     */
    private enum OperatingSystem {
        WINDOWS, LINUX, MACOS
    }
    
    /**
     * Architecture enumeration
     */
    private enum Architecture {
        X64, ARM64
    }
    
    /**
     * System information container
     */
    private static class SystemInfo {
        final OperatingSystem os;
        final Architecture arch;
        
        SystemInfo(OperatingSystem os, Architecture arch) {
            this.os = os;
            this.arch = arch;
        }
        
        @Override
        public String toString() {
            return os + "_" + arch;
        }
    }
    
    /**
     * Gets the version of the downloaded/available FFmpeg
     */
    public static CompletableFuture<String> getFFmpegVersion() {
        return getFFmpegPath().thenCompose(path -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(path.toString(), "-version");
                    Process process = pb.start();
                    
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String firstLine = reader.readLine();
                        if (firstLine != null && firstLine.contains("ffmpeg version")) {
                            return firstLine;
                        }
                    }
                    
                    return "Unknown version";
                } catch (Exception e) {
                    return "Error getting version: " + e.getMessage();
                }
            });
        });
    }
    
    /**
     * Cleans up downloaded FFmpeg files
     */
    public static void cleanup() {
        try {
            Path ffmpegDir = Paths.get(FFMPEG_DIR);
            if (Files.exists(ffmpegDir)) {
                System.out.println("Cleaning up FFmpeg directory: " + ffmpegDir);
                Files.walk(ffmpegDir)
                        .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                                System.out.println("Deleted: " + path.getFileName());
                            } catch (IOException e) {
                                System.err.println("Failed to delete: " + path + " - " + e.getMessage());
                            }
                        });
                System.out.println("✅ FFmpeg cleanup complete");
            }
            isDownloaded = false;
            ffmpegPath = null;
        } catch (IOException e) {
            System.err.println("Failed to cleanup FFmpeg directory: " + e.getMessage());
        }
    }
    
    /**
     * Gets information about the current FFmpeg installation
     */
    public static void printInstallationInfo() {
        System.out.println("=== FFmpeg Installation Info ===");
        System.out.println("FFmpeg Directory: " + FFMPEG_DIR);
        System.out.println("Executable Name: " + FFMPEG_EXECUTABLE_NAME);
        System.out.println("Is Downloaded: " + isDownloaded);
        System.out.println("Is Available: " + isFFmpegAvailable());
        
        if (ffmpegPath != null) {
            System.out.println("FFmpeg Path: " + ffmpegPath);
            System.out.println("File Exists: " + Files.exists(ffmpegPath));
            if (Files.exists(ffmpegPath)) {
                try {
                    System.out.println("File Size: " + formatBytes(Files.size(ffmpegPath)));
                    System.out.println("Is Executable: " + Files.isExecutable(ffmpegPath));
                } catch (IOException e) {
                    System.err.println("Error reading file info: " + e.getMessage());
                }
            }
        }
        
        // Check for ffprobe
        Path ffprobeDir = Paths.get(FFMPEG_DIR);
        Path ffprobePath = ffprobeDir.resolve(getProbeExecutableName());
        System.out.println("FFprobe Available: " + Files.exists(ffprobePath));
        if (Files.exists(ffprobePath)) {
            try {
                System.out.println("FFprobe Size: " + formatBytes(Files.size(ffprobePath)));
                System.out.println("FFprobe Executable: " + Files.isExecutable(ffprobePath));
            } catch (IOException e) {
                System.err.println("Error reading FFprobe info: " + e.getMessage());
            }
        }
        
        System.out.println("================================");
    }
}