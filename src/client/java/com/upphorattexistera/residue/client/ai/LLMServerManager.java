package com.upphorattexistera.residue.client.ai;

import com.upphorattexistera.residue.config.DownloadStatusLabel;
import com.upphorattexistera.residue.config.LLMBackend;
import com.upphorattexistera.residue.config.LLMModel;
import com.upphorattexistera.residue.config.ResidueConfig;
import net.minecraft.text.Text;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LLMServerManager {

    // -------------------------------------------------------------------------
    // Singleton (thread-safe через holder)
    // -------------------------------------------------------------------------

    private static class Holder {
        static final LLMServerManager INSTANCE = new LLMServerManager();
    }

    public static LLMServerManager getInstance() {
        return Holder.INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private Process serverProcess;
    private boolean isRunning = false;

    private final Path baseDir   = Paths.get("residue_ai");
    private final Path serverDir = baseDir.resolve("server");

    /** Status label in UI (maybe null if settings screen hasn't been opened) */
    private volatile DownloadStatusLabel statusLabel;

    private final AtomicBoolean downloadCancelled = new AtomicBoolean(false);
    private volatile Thread downloadThread;

    private static final String[] MIRRORS = {
            "",
            "https://hf-mirror.com",
            "https://alpha.hf-mirror.com"
    };

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

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
        System.out.println("[ResidueAI] Download cancelled by user");
    }

    // -------------------------------------------------------------------------
    // Download and Setup
    // -------------------------------------------------------------------------

    public void downloadAndSetup() throws Exception {
        downloadThread = Thread.currentThread();
        downloadCancelled.set(false);

        if (statusLabel != null) statusLabel.setProgress(0);

        Files.createDirectories(serverDir);
        Files.createDirectories(baseDir);

        LLMModel model = ResidueConfig.INSTANCE.llmModel;
        Path resolvedModelPath;
        String modelUrl;

        if (model == LLMModel.CUSTOM) {
            String customName = ResidueConfig.INSTANCE.customModelName;
            if (customName == null || customName.isBlank()) {
                if (statusLabel != null) statusLabel.setError(
                        Text.translatable("residue.error.custom_model_required").getString());
                throw new Exception(
                        "Custom model: please specify filename (customModelName) in settings. " +
                                "File should be placed in: " + baseDir.toAbsolutePath());
            }
            resolvedModelPath = baseDir.resolve(customName);
            modelUrl = "";
        } else {
            resolvedModelPath = baseDir.resolve(model.fileName);
            modelUrl = model.downloadUrl;
        }

        boolean needBackend = !Files.exists(serverDir.resolve("llama-server.exe"));
        boolean needModel   = !Files.exists(resolvedModelPath) && !modelUrl.isEmpty();

        if (!needBackend && !needModel) {
            System.out.println("[ResidueAI] All files already exist, skipping download");
            if (statusLabel != null) statusLabel.setState(DownloadStatusLabel.State.DONE);
            return;
        }

        int totalSteps  = (needBackend ? 1 : 0) + (needModel ? 1 : 0);
        int currentStep = 0;

        if (needBackend) {
            checkCancelled();

            LLMBackend backend = ResidueConfig.INSTANCE.llmBackend;
            if (backend == LLMBackend.AUTO) backend = detectBestBackend();

            String zipUrl = backend.getDownloadUrl();
            if (zipUrl == null || zipUrl.isBlank())
                throw new Exception("Failed to get download URL for backend: " + backend.displayName);

            System.out.println("[ResidueAI] Downloading backend: " + backend.displayName);
            Path zipPath = baseDir.resolve("llama-server.zip");

            try {
                downloadFile(zipUrl, zipPath, currentStep, totalSteps);
                checkCancelled();
                extractZip(zipPath, serverDir);
            } finally {
                Files.deleteIfExists(zipPath);
            }

            currentStep++;
            System.out.println("[ResidueAI] Backend ready");
        }

        if (needModel) {
            checkCancelled();
            System.out.println("[ResidueAI] Downloading model: " + model.displayName +
                    " (~" + model.sizeMB + " MB)");
            downloadFile(modelUrl, resolvedModelPath, currentStep, totalSteps);
            System.out.println("[ResidueAI] Model downloaded: " + resolvedModelPath.getFileName());
        } else if (model == LLMModel.CUSTOM) {
            System.out.println("[ResidueAI] Custom model: place file at " + resolvedModelPath);
        }

        if (statusLabel != null) statusLabel.setState(DownloadStatusLabel.State.DONE);
        System.out.println("[ResidueAI] Setup complete");
    }

    // -------------------------------------------------------------------------
    // Server Start / Stop
    // -------------------------------------------------------------------------

    public void startServer() throws Exception {
        if (isRunning()) {
            System.out.println("[ResidueAI] Server is already running");
            return;
        }

        LLMModel model = ResidueConfig.INSTANCE.llmModel;
        Path resolvedModelPath;

        if (model == LLMModel.CUSTOM) {
            String customName = ResidueConfig.INSTANCE.customModelName;
            if (customName == null || customName.isBlank())
                throw new RuntimeException("Custom model: please specify filename in settings.");
            resolvedModelPath = baseDir.resolve(customName);
        } else {
            resolvedModelPath = baseDir.resolve(model.fileName);
        }

        Path exePath = serverDir.resolve("llama-server.exe");

        if (!Files.exists(exePath))
            throw new RuntimeException(
                    "llama-server.exe not found. Please download the backend first via settings menu.");
        if (!Files.exists(resolvedModelPath))
            throw new RuntimeException(
                    "Model file not found: " + resolvedModelPath.toAbsolutePath());

        LLMBackend backend = ResidueConfig.INSTANCE.llmBackend;
        if (backend == LLMBackend.AUTO) backend = detectBestBackend();

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(serverDir.toFile());
        pb.command(
                exePath.toAbsolutePath().toString(),
                "-m", resolvedModelPath.toAbsolutePath().toString(),
                "--port", "8080",
                "--ctx-size", String.valueOf(model.contextSize()),
                "--threads", String.valueOf(Runtime.getRuntime().availableProcessors())
        );

        if (backend == LLMBackend.VULKAN || backend == LLMBackend.CUDA12 ||
                backend == LLMBackend.CUDA13 || backend == LLMBackend.HIP) {
            pb.command().add("--n-gpu-layers");
            pb.command().add("99");
        }

        System.out.println("[ResidueAI] Starting: " + String.join(" ", pb.command()));
        serverProcess = pb.start();
        isRunning = true;

        startLogReader(serverProcess.getInputStream(), "[LLAMA]",     false);
        startLogReader(serverProcess.getErrorStream(),  "[LLAMA-ERR]", true);

        waitForServer(60_000);
        System.out.println("[ResidueAI] Server is ready on port 8080");
    }

    public void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
            System.out.println("[ResidueAI] Server stopped");
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

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

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

                System.out.printf("[ResidueAI] Attempt %d/%d failed. Retrying in %d seconds...%n",
                        attempt, maxRetries, retryDelayMs/1000);

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

            System.out.println("[ResidueAI] Attempting download from: " + currentUrl);

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
                        System.out.println("[ResidueAI] 429 Rate Limit. Waiting " + waitSeconds + " seconds... (mirror: " + (mirrorBase.isEmpty() ? "original" : mirrorBase) + ")");
                        Thread.sleep(waitSeconds * 1000);
                        break;
                    }

                    if (responseCode != 200) {
                        String body = readErrorBody(connection);
                        throw new IOException("HTTP " + responseCode + " from " + currentUrl +
                                (body.isEmpty() ? "" : ": " + body));
                    }

                    String contentType = connection.getContentType();
                    if (contentType != null &&
                            (contentType.contains("text/html") || contentType.contains("application/json"))) {
                        throw new IOException("Received HTML/JSON instead of file. Check your HF token.");
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
                        throw new IOException("Downloaded file is empty");
                    }

                    System.out.println("[ResidueAI] Successfully downloaded: " + destination.getFileName() +
                            " (" + (actualSize / (1024 * 1024)) + " MB) from " + currentUrl);
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

        throw new IOException("Failed to download file from all sources (all mirrors failed)");
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
            System.out.println("[ResidueAI] Using Hugging Face token");
        } else {
            System.out.println("[ResidueAI] Warning: HF token is not set — high risk of 429 errors");
        }

        return conn;
    }

    private String readErrorBody(HttpURLConnection conn) {
        try (InputStream err = conn.getErrorStream()) {
            if (err == null) return "";
            return new String(err.readNBytes(500)).replaceAll("\\s+", " ").strip();
        } catch (Exception ignored) { return ""; }
    }

    private void extractZip(Path zipPath, Path destDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipPath.toFile())))) {
            ZipEntry entry;
            byte[] buffer = new byte[65536];
            while ((entry = zis.getNextEntry()) != null) {
                checkCancelled();
                Path filePath = destDir.resolve(entry.getName()).normalize();
                if (!filePath.startsWith(destDir.normalize()))
                    throw new SecurityException("Zip-slip attempt: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
                    }
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
                throw new RuntimeException("llama-server exited immediately after start. Check [LLAMA-ERR] logs.");
            }

            try {
                URL healthUrl = new URL("http://localhost:8080/health");
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
                System.out.println("[ResidueAI] Still waiting for server to respond... (" + attempt + "s)");
            }
        }

        throw new RuntimeException(
                "llama-server did not respond within " + (timeoutMs / 1000) + " seconds. " +
                        "Possibly insufficient RAM or VRAM.");
    }

    private void startLogReader(InputStream stream, String prefix, boolean isError) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isError) System.err.println(prefix + " " + line);
                    else         System.out.println(prefix + " " + line);
                }
            } catch (IOException e) {
                if (isRunning()) System.err.println("[ResidueAI] Log reader error: " + e.getMessage());
            }
        }, "LLM-" + prefix);
        t.setDaemon(true);
        t.start();
    }

    private void checkCancelled() throws InterruptedException {
        if (downloadCancelled.get() || Thread.currentThread().isInterrupted())
            throw new InterruptedException("Download cancelled");
    }

    private LLMBackend detectBestBackend() {
        return LLMBackend.VULKAN;
    }
}