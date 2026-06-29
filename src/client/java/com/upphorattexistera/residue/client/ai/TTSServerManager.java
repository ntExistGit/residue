package com.upphorattexistera.residue.client.ai;

import com.upphorattexistera.residue.config.DownloadStatusLabel;
import com.upphorattexistera.residue.config.LLMBackend;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.config.TTSTalker;
import com.upphorattexistera.residue.config.TTSTokenizer;
import net.minecraft.text.Text;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TTSServerManager {
    private static class Holder {
        static final TTSServerManager INSTANCE = new TTSServerManager();
    }

    public static TTSServerManager getInstance() {
        return Holder.INSTANCE;
    }

    private Process serverProcess;
    private boolean isRunning = false;

    private final Path baseDir = Paths.get("residue_ai");
    private final Path ttsDir = baseDir.resolve("tts_server");
    private final Path modelsDir = baseDir.resolve("models/tts");

    private volatile DownloadStatusLabel statusLabel;
    private final AtomicBoolean downloadCancelled = new AtomicBoolean(false);
    private volatile Thread downloadThread;

    private static final String[] MIRRORS = {
            "",
            "https://hf-mirror.com",
            "https://alpha.hf-mirror.com"
    };

    public void setStatusLabel(DownloadStatusLabel label) {
        this.statusLabel = label;
    }

    public Optional<DownloadStatusLabel> getStatusLabel() {
        return Optional.ofNullable(statusLabel);
    }

    public void cancelDownload() {
        downloadCancelled.set(true);
        Thread t = downloadThread;
        if (t != null && t.isAlive()) t.interrupt();
        if (statusLabel != null) statusLabel.setState(DownloadStatusLabel.State.CANCELLED);
        System.out.println("[ResidueAI] TTS download cancelled by user");
    }

    public void downloadAndSetup() throws Exception {
        downloadThread = Thread.currentThread();
        downloadCancelled.set(false);

        if (statusLabel != null) statusLabel.setProgress(0);

        Files.createDirectories(ttsDir);
        Files.createDirectories(modelsDir);

        // Tokenizer
        TTSTokenizer tokenizer = ResidueConfig.INSTANCE.ttsTokenizer;
        Path resolvedTokenizerPath;
        String tokenizerUrl;
        if (tokenizer == TTSTokenizer.CUSTOM) {
            String customName = ResidueConfig.INSTANCE.ttsCustomTokenizerName;
            if (customName == null || customName.isBlank()) {
                if (statusLabel != null) statusLabel.setError("Custom tokenizer name is empty");
                throw new Exception("Custom tokenizer: please specify filename in settings.");
            }
            resolvedTokenizerPath = modelsDir.resolve(customName);
            tokenizerUrl = "";
        } else {
            resolvedTokenizerPath = modelsDir.resolve(tokenizer.fileName);
            tokenizerUrl = tokenizer.downloadUrl;
        }

        // Talker
        TTSTalker talker = ResidueConfig.INSTANCE.ttsTalker;
        Path resolvedTalkerPath;
        String talkerUrl;
        if (talker == TTSTalker.CUSTOM) {
            String customName = ResidueConfig.INSTANCE.ttsCustomTalkerName;
            if (customName == null || customName.isBlank()) {
                if (statusLabel != null) statusLabel.setError("Custom talker name is empty");
                throw new Exception("Custom talker: please specify filename in settings.");
            }
            resolvedTalkerPath = modelsDir.resolve(customName);
            talkerUrl = "";
        } else {
            resolvedTalkerPath = modelsDir.resolve(talker.fileName);
            talkerUrl = talker.downloadUrl;
        }

        boolean needBackend = !Files.exists(ttsDir.resolve("tts-server.exe"));
        boolean needTokenizer = !Files.exists(resolvedTokenizerPath) && !tokenizerUrl.isEmpty();
        boolean needTalker = !Files.exists(resolvedTalkerPath) && !talkerUrl.isEmpty();

        if (!needBackend && !needTokenizer && !needTalker) {
            System.out.println("[ResidueAI] TTS files already exist, skipping download");
            if (statusLabel != null) statusLabel.setState(DownloadStatusLabel.State.DONE);
            return;
        }

        int totalSteps = (needBackend ? 1 : 0) + (needTokenizer ? 1 : 0) + (needTalker ? 1 : 0);
        int currentStep = 0;

        // 1. Backend
        if (needBackend) {
            checkCancelled();
            String zipUrl = "https://github.com/ntExistGit/qwentts.cpp/releases/download/1.0/qwen-tts.zip";
            System.out.println("[ResidueAI] Downloading TTS backend...");
            Path zipPath = baseDir.resolve("qwen-tts.zip");

            try {
                downloadFile(zipUrl, zipPath, currentStep, totalSteps);
                checkCancelled();
                extractZipFlat(zipPath, ttsDir); // Распаковываем в корень ttsDir
            } finally {
                Files.deleteIfExists(zipPath);
            }
            currentStep++;
            System.out.println("[ResidueAI] TTS backend ready");
        }

        // 2. Tokenizer
        if (needTokenizer) {
            checkCancelled();
            System.out.println("[ResidueAI] Downloading TTS tokenizer: " + tokenizer.displayName +
                    " (~" + tokenizer.sizeMB + " MB)");
            downloadFile(tokenizerUrl, resolvedTokenizerPath, currentStep, totalSteps);
            currentStep++;
            System.out.println("[ResidueAI] Tokenizer downloaded");
        }

        // 3. Talker
        if (needTalker) {
            checkCancelled();
            System.out.println("[ResidueAI] Downloading TTS talker: " + talker.displayName +
                    " (~" + talker.sizeMB + " MB)");
            downloadFile(talkerUrl, resolvedTalkerPath, currentStep, totalSteps);
            System.out.println("[ResidueAI] Talker downloaded");
        }

        if (statusLabel != null) statusLabel.setState(DownloadStatusLabel.State.DONE);
        System.out.println("[ResidueAI] TTS setup complete");
    }

    public void startServer() throws Exception {
        if (isRunning()) {
            System.out.println("[ResidueAI] TTS server is already running");
            return;
        }

        // Tokenizer path
        TTSTokenizer tokenizer = ResidueConfig.INSTANCE.ttsTokenizer;
        Path resolvedTokenizerPath;
        if (tokenizer == TTSTokenizer.CUSTOM) {
            resolvedTokenizerPath = modelsDir.resolve(ResidueConfig.INSTANCE.ttsCustomTokenizerName);
        } else {
            resolvedTokenizerPath = modelsDir.resolve(tokenizer.fileName);
        }

        // Talker path
        TTSTalker talker = ResidueConfig.INSTANCE.ttsTalker;
        Path resolvedTalkerPath;
        if (talker == TTSTalker.CUSTOM) {
            resolvedTalkerPath = modelsDir.resolve(ResidueConfig.INSTANCE.ttsCustomTalkerName);
        } else {
            resolvedTalkerPath = modelsDir.resolve(talker.fileName);
        }

        Path exePath = ttsDir.resolve("tts-server.exe");

        if (!Files.exists(exePath))
            throw new RuntimeException("tts-server.exe not found. Please download TTS backend first.");
        if (!Files.exists(resolvedTokenizerPath))
            throw new RuntimeException("Tokenizer file not found: " + resolvedTokenizerPath.toAbsolutePath());
        if (!Files.exists(resolvedTalkerPath))
            throw new RuntimeException("Talker file not found: " + resolvedTalkerPath.toAbsolutePath());

        LLMBackend backend = ResidueConfig.INSTANCE.llmBackend;
        if (backend == LLMBackend.AUTO) backend = detectBestBackend();

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(ttsDir.toFile());
        pb.command(
                exePath.toAbsolutePath().toString(),
                "--model", resolvedTokenizerPath.toAbsolutePath().toString(),
                "--talker", resolvedTalkerPath.toAbsolutePath().toString(),
                "--port", "8081",
                "--threads", String.valueOf(Runtime.getRuntime().availableProcessors())
        );

        if (backend == LLMBackend.VULKAN || backend == LLMBackend.CUDA12 ||
                backend == LLMBackend.CUDA13 || backend == LLMBackend.HIP) {
            pb.command().add("--n-gpu-layers");
            pb.command().add("99");
        }

        System.out.println("[ResidueAI] Starting TTS: " + String.join(" ", pb.command()));
        serverProcess = pb.start();
        isRunning = true;

        startLogReader(serverProcess.getInputStream(), "[QWEN-TTS]", false);
        startLogReader(serverProcess.getErrorStream(), "[QWEN-TTS-ERR]", true);

        waitForServer(60_000);
        System.out.println("[ResidueAI] TTS server is ready on port 8081");
    }

    public void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
            System.out.println("[ResidueAI] TTS server stopped");
        }
        isRunning = false;
        serverProcess = null;
    }

    public boolean isRunning() {
        if (serverProcess != null && !serverProcess.isAlive()) {
            isRunning = false;
            serverProcess = null;
        }
        return isRunning;
    }

    private void downloadFile(String fileUrl, Path destination,
                              int currentStep, int totalSteps) throws Exception {
        if (Files.exists(destination) && Files.size(destination) == 0) {
            Files.delete(destination);
        }

        int maxRetries = 7;
        int retryDelayMs = 4000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                attemptDownload(fileUrl, destination, currentStep, totalSteps, attempt, maxRetries);
                return;
            } catch (IOException e) {
                if (attempt == maxRetries) throw e;

                System.out.printf("[ResidueAI] TTS download attempt %d/%d failed. Retrying in %d seconds...%n",
                        attempt, maxRetries, retryDelayMs / 1000);

                Thread.sleep(retryDelayMs);
                retryDelayMs = Math.min(retryDelayMs * 2, 45000);
            }
        }
    }

    private void attemptDownload(String fileUrl, Path destination,
                                 int currentStep, int totalSteps,
                                 int attempt, int maxRetries) throws Exception {
        for (String mirrorBase : MIRRORS) {
            String currentUrl = mirrorBase.isEmpty()
                    ? fileUrl
                    : fileUrl.replace("https://huggingface.co", mirrorBase);

            System.out.println("[ResidueAI] Attempting TTS download from: " + currentUrl);

            URL url = new URL(currentUrl);
            int redirects = 0;
            final int MAX_REDIRECTS = 15;

            while (redirects++ < MAX_REDIRECTS) {
                HttpURLConnection connection = null;
                try {
                    connection = openConnection(url);
                    int responseCode = connection.getResponseCode();

                    if (responseCode >= 300 && responseCode < 400) {
                        String location = connection.getHeaderField("Location");
                        if (location == null || location.isBlank()) {
                            throw new IOException("Redirect without Location header from " + currentUrl);
                        }
                        System.out.println("[ResidueAI] Redirect → " + location);
                        url = new URL(url, location);
                        continue;
                    }

                    if (responseCode == 429) {
                        String retryAfter = connection.getHeaderField("Retry-After");
                        long waitSeconds = 8;
                        if (retryAfter != null) {
                            try {
                                waitSeconds = Long.parseLong(retryAfter);
                            } catch (Exception ignored) {}
                        }
                        System.out.println("[ResidueAI] 429 Rate Limit. Waiting " + waitSeconds + " seconds...");
                        Thread.sleep(waitSeconds * 1000);
                        break;
                    }

                    if (responseCode != 200) {
                        String body = readErrorBody(connection);
                        throw new IOException("HTTP " + responseCode + " from " + currentUrl +
                                (body.isEmpty() ? "" : ": " + body));
                    }

                    long fileSize = connection.getContentLengthLong();
                    long downloaded = 0;

                    try (InputStream in = new BufferedInputStream(connection.getInputStream(), 131072);
                         OutputStream out = new BufferedOutputStream(
                                 new FileOutputStream(destination.toFile()), 131072)) {

                        byte[] buffer = new byte[131072];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            checkCancelled();
                            out.write(buffer, 0, bytesRead);
                            downloaded += bytesRead;

                            if (fileSize > 0) {
                                int fileProgress = (int) (downloaded * 100L / fileSize);
                                int overallProgress = (int) ((currentStep * 100L + fileProgress) / totalSteps);
                                if (statusLabel != null) statusLabel.setProgress(overallProgress);
                            }
                        }
                    }

                    long actualSize = Files.size(destination);
                    if (actualSize == 0) {
                        Files.deleteIfExists(destination);
                        throw new IOException("Downloaded TTS file is empty");
                    }

                    System.out.println("[ResidueAI] Successfully downloaded TTS: " + destination.getFileName() +
                            " (" + (actualSize / (1024 * 1024)) + " MB)");
                    return;

                } catch (IOException e) {
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        break;
                    }
                    throw e;
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        }
        throw new IOException("Failed to download TTS file from all sources");
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(300_000);
        conn.setInstanceFollowRedirects(false);

        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Encoding", "identity");

        String token = ResidueConfig.INSTANCE.huggingFaceToken;
        if (token != null && !token.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + token.strip());
        }

        return conn;
    }

    private String readErrorBody(HttpURLConnection conn) {
        try (InputStream err = conn.getErrorStream()) {
            if (err == null) return "";
            return new String(err.readNBytes(500)).replaceAll("\\s+", " ").strip();
        } catch (Exception ignored) { return ""; }
    }

    // Распаковка ZIP в корень (игнорируем пути внутри архива)
    private void extractZipFlat(Path zipPath, Path destDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipPath.toFile())))) {
            ZipEntry entry;
            byte[] buffer = new byte[65536];
            while ((entry = zis.getNextEntry()) != null) {
                checkCancelled();
                String fileName = Paths.get(entry.getName()).getFileName().toString();
                if (fileName.isEmpty() || entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                Path filePath = destDir.resolve(fileName).normalize();
                if (!filePath.startsWith(destDir.normalize()))
                    throw new SecurityException("Zip-slip attempt: " + entry.getName());

                try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
                }
                zis.closeEntry();
            }
        }
    }

    private void waitForServer(long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int attempt = 0;

        while (System.currentTimeMillis() < deadline) {
            if (!isRunning()) {
                throw new RuntimeException("tts-server exited immediately after start.");
            }

            try {
                URL healthUrl = new URL("http://localhost:8081/health");
                HttpURLConnection c = (HttpURLConnection) healthUrl.openConnection();
                c.setConnectTimeout(2000);
                c.setReadTimeout(2000);

                if (c.getResponseCode() == 200) {
                    c.disconnect();
                    return;
                }
                c.disconnect();
            } catch (IOException ignored) {}

            Thread.sleep(1000);
            attempt++;

            if (attempt % 10 == 0) {
                System.out.println("[ResidueAI] Still waiting for TTS server... (" + attempt + "s)");
            }
        }
        throw new RuntimeException("TTS server did not respond within " + (timeoutMs / 1000) + " seconds.");
    }

    private void startLogReader(InputStream stream, String prefix, boolean isError) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isError) System.err.println(prefix + " " + line);
                    else System.out.println(prefix + " " + line);
                }
            } catch (IOException e) {
                if (isRunning()) System.err.println("[ResidueAI] TTS log reader error: " + e.getMessage());
            }
        }, "TTS-" + prefix);
        t.setDaemon(true);
        t.start();
    }

    private void checkCancelled() throws InterruptedException {
        if (downloadCancelled.get() || Thread.currentThread().isInterrupted())
            throw new InterruptedException("TTS download cancelled");
    }

    private LLMBackend detectBestBackend() {
        return LLMBackend.VULKAN;
    }
}