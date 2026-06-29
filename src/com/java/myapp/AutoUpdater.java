package com.java.myapp;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public final class AutoUpdater {

    private static final String APP_NAME = "CreateWorkOrderProms";
    private static final String RELEASE_DOWNLOAD_URL = "https://github.com/kantapon-sam/CreateWorkOrderProms/releases/download/";
    private static final int TIMEOUT_MS = 30000;

    private AutoUpdater() {
    }

    public static void main(String[] args) {
        String latestVersion = args.length > 0 ? args[0] : "";
        File appFolder = args.length > 1 ? new File(args[1]) : AppPaths.getAppFolder();
        UpdateProgressWindow progress = new UpdateProgressWindow();
        progress.showWindow();

        try {
            progress.setStatus("Preparing update...");
            progress.setProgress(3, "Preparing");
            Thread.sleep(2500L);
            File updateDir = createUpdateDir();
            File zipFile = new File(updateDir, APP_NAME + ".zip");
            File payloadDir = new File(updateDir, "payload");

            downloadFile(downloadUrlForVersion(latestVersion), zipFile, progress);
            unzip(zipFile, payloadDir, progress);
            copyPayload(payloadDir, appFolder.getCanonicalFile(), progress);

            progress.setStatus("Update complete.");
            progress.setProgress(100, "Complete");
            JOptionPane.showMessageDialog(
                    progress.getFrame(),
                    "Update complete" + (latestVersion.trim().isEmpty() ? "." : " to v" + latestVersion + ".")
                    + "\nClick OK to open CreateWorkOrderProms.",
                    APP_NAME + " Update",
                    JOptionPane.INFORMATION_MESSAGE);
            launchUpdatedApp(appFolder.getCanonicalFile());
            progress.closeWindow();
        } catch (Exception ex) {
            showFailure(ex, progress);
        }
    }

    private static File createUpdateDir() throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"), APP_NAME + "-update-" + System.currentTimeMillis());
        if (!dir.mkdirs()) {
            throw new IOException("Cannot create update folder: " + dir.getAbsolutePath());
        }
        return dir;
    }

    private static String downloadUrlForVersion(String version) throws IOException {
        String normalizedVersion = normalizeVersion(version);
        if (normalizedVersion.isEmpty()) {
            throw new IOException("Latest version was not provided.");
        }
        return RELEASE_DOWNLOAD_URL
                + "v" + normalizedVersion
                + "/"
                + APP_NAME + "-v" + normalizedVersion + ".zip";
    }

    private static String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }
        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized.replaceAll("[^0-9.].*$", "");
    }

    private static void downloadFile(String urlString, File destination, UpdateProgressWindow progress) throws IOException {
        progress.setStatus("Downloading update...");
        progress.setIndeterminate("Connecting");
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", APP_NAME + "-AutoUpdater");

        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            throw new IOException("Download failed with HTTP " + responseCode
                    + ". Please upload the versioned CreateWorkOrderProms zip file to the GitHub Release.");
        }

        long totalBytes = connection.getContentLengthLong();
        progress.setDownloadProgress(0L, totalBytes);
        try (InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            long bytesRead = 0L;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                bytesRead += read;
                progress.setDownloadProgress(bytesRead, totalBytes);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void unzip(File zipFile, File destinationFolder, UpdateProgressWindow progress) throws IOException {
        progress.setStatus("Extracting update...");
        progress.setIndeterminate("Extracting");
        byte[] buffer = new byte[8192];
        if (!destinationFolder.mkdirs() && !destinationFolder.exists()) {
            throw new IOException("Cannot create unzip folder: " + destinationFolder.getAbsolutePath());
        }

        try (ZipInputStream zipInput = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                progress.setStatus("Extracting " + shortName(entry.getName()) + "...");
                File outputFile = safeZipOutputFile(destinationFolder, entry);
                if (entry.isDirectory()) {
                    if (!outputFile.mkdirs() && !outputFile.exists()) {
                        throw new IOException("Cannot create folder: " + outputFile.getAbsolutePath());
                    }
                } else {
                    File parent = outputFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Cannot create folder: " + parent.getAbsolutePath());
                    }
                    try (FileOutputStream output = new FileOutputStream(outputFile)) {
                        int read;
                        while ((read = zipInput.read(buffer)) != -1) {
                            output.write(buffer, 0, read);
                        }
                    }
                }
                zipInput.closeEntry();
            }
        }
        progress.setProgress(78, "Extracted");
    }

    private static File safeZipOutputFile(File destinationFolder, ZipEntry entry) throws IOException {
        File outputFile = new File(destinationFolder, entry.getName()).getCanonicalFile();
        String destinationPath = destinationFolder.getCanonicalPath() + File.separator;
        if (!outputFile.getPath().startsWith(destinationPath)) {
            throw new IOException("Blocked unsafe zip entry: " + entry.getName());
        }
        return outputFile;
    }

    private static void copyPayload(File sourceFolder, File appFolder, UpdateProgressWindow progress) throws IOException {
        progress.setStatus("Installing update...");
        int totalFiles = Math.max(countCopyFiles(sourceFolder, sourceFolder.getCanonicalPath()), 1);
        copyChildren(sourceFolder, appFolder, sourceFolder.getCanonicalPath(), new int[]{0}, totalFiles, progress);
    }

    private static void copyChildren(File source, File destination, String sourceRootPath,
            int[] copiedFiles, int totalFiles, UpdateProgressWindow progress) throws IOException {
        File[] children = source.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String relativePath = child.getCanonicalPath().substring(sourceRootPath.length() + 1);
            if (shouldSkip(relativePath)) {
                continue;
            }

            File target = new File(destination, relativePath);
            if (child.isDirectory()) {
                if (!target.exists() && !target.mkdirs()) {
                    throw new IOException("Cannot create folder: " + target.getAbsolutePath());
                }
                copyChildren(child, destination, sourceRootPath, copiedFiles, totalFiles, progress);
            } else {
                File parent = target.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Cannot create folder: " + parent.getAbsolutePath());
                }
                Files.copy(child.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copiedFiles[0]++;
                int percent = 80 + Math.min(17, (copiedFiles[0] * 17) / totalFiles);
                progress.setStatus("Installing " + shortName(relativePath) + "...");
                progress.setProgress(percent, copiedFiles[0] + "/" + totalFiles);
            }
        }
    }

    private static int countCopyFiles(File source, String sourceRootPath) throws IOException {
        File[] children = source.listFiles();
        if (children == null) {
            return 0;
        }

        int count = 0;
        for (File child : children) {
            String relativePath = child.getCanonicalPath().substring(sourceRootPath.length() + 1);
            if (shouldSkip(relativePath)) {
                continue;
            }
            if (child.isDirectory()) {
                count += countCopyFiles(child, sourceRootPath);
            } else {
                count++;
            }
        }
        return count;
    }

    private static boolean shouldSkip(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        return normalized.equals("ListWorkOrder.csv")
                || normalized.equals("Login.txt")
                || normalized.equals("site_found.txt")
                || normalized.equals("User")
                || normalized.startsWith("User/")
                || normalized.equals(".git")
                || normalized.startsWith(".git/")
                || normalized.equals("run-logs")
                || normalized.startsWith("run-logs/")
                || normalized.equals("release")
                || normalized.startsWith("release/");
    }

    private static void launchUpdatedApp(File appFolder) throws IOException {
        File appJar = new File(appFolder, APP_NAME + ".jar");
        if (!appJar.exists()) {
            throw new IOException("Updated app was not found: " + appJar.getAbsolutePath());
        }

        new ProcessBuilder(getJavaExecutable(), "-jar", appJar.getAbsolutePath())
                .directory(appFolder)
                .start();
    }

    private static String getJavaExecutable() {
        File javaw = new File(new File(System.getProperty("java.home"), "bin"), "javaw.exe");
        if (javaw.exists()) {
            return javaw.getAbsolutePath();
        }
        return new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath();
    }

    private static String shortName(String path) {
        String normalized = path.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < normalized.length()) {
            return normalized.substring(slash + 1);
        }
        return normalized;
    }

    private static void showFailure(Exception ex, UpdateProgressWindow progress) {
        try {
            JOptionPane.showMessageDialog(
                    progress.getFrame(),
                    "Update failed:\n" + ex.getMessage(),
                    APP_NAME + " Update",
                    JOptionPane.ERROR_MESSAGE);
            progress.closeWindow();
        } catch (Exception ignored) {
            System.out.println("Update failed: " + ex.toString());
        }
    }

    private static final class UpdateProgressWindow {

        private final JFrame frame;
        private final JLabel statusLabel;
        private final JProgressBar progressBar;

        UpdateProgressWindow() {
            frame = new JFrame(APP_NAME + " Update");
            statusLabel = new JLabel("Starting update...");
            progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressBar.setString("Starting");

            JPanel panel = new JPanel(new GridLayout(2, 1, 6, 8));
            panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
            panel.add(statusLabel);
            panel.add(progressBar);

            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            frame.setSize(440, 140);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        }

        JFrame getFrame() {
            return frame;
        }

        void showWindow() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    frame.setVisible(true);
                }
            });
        }

        void setStatus(final String message) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    statusLabel.setText(message);
                }
            });
        }

        void setProgress(final int value, final String text) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(Math.max(0, Math.min(100, value)));
                    progressBar.setString(text);
                }
            });
        }

        void setIndeterminate(final String text) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    progressBar.setIndeterminate(true);
                    progressBar.setString(text);
                }
            });
        }

        void setDownloadProgress(final long bytesRead, final long totalBytes) {
            if (totalBytes <= 0L) {
                setIndeterminate(formatBytes(bytesRead));
                return;
            }

            final int downloadPercent = (int) Math.min(100L, (bytesRead * 100L) / totalBytes);
            final int overallPercent = 8 + (int) Math.min(62L, (bytesRead * 62L) / totalBytes);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(overallPercent);
                    progressBar.setString(downloadPercent + "% (" + formatBytes(bytesRead) + " / " + formatBytes(totalBytes) + ")");
                }
            });
        }

        void closeWindow() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    frame.dispose();
                }
            });
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024L) {
                return bytes + " B";
            }
            double kb = bytes / 1024.0;
            if (kb < 1024.0) {
                return String.format("%.1f KB", kb);
            }
            double mb = kb / 1024.0;
            return String.format("%.1f MB", mb);
        }
    }
}
