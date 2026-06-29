package com.java.myapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.swing.JOptionPane;

public final class AutoUpdateChecker {

    private static final String VERSION_URL = "https://raw.githubusercontent.com/kantapon-sam/CreateWorkOrderProms/main/VERSION";
    private static final String UPDATE_JAR_NAME = "AutoUpdater.jar";
    private static final String UPDATE_JAR_PATH = "lib" + File.separator + UPDATE_JAR_NAME;
    private static final int TIMEOUT_MS = 7000;

    private AutoUpdateChecker() {
    }

    public static void checkForUpdate(ProgressWindow progress) {
        if (Boolean.getBoolean("createworkorderproms.skipUpdate")) {
            return;
        }

        try {
            progress.setStatus("Checking for updates...");
            String latestVersion = readLatestVersion();
            if (isNewerVersion(latestVersion, AppVersion.VERSION)) {
                int result = JOptionPane.showConfirmDialog(
                        null,
                        "New version " + formatVersion(latestVersion) + " is available.\n"
                        + "Current version is " + AppVersion.displayVersion() + ".\n\n"
                        + "Update now?",
                        "CreateWorkOrderProms Update",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    startUpdater(latestVersion);
                    System.exit(0);
                }
            }
        } catch (Exception ex) {
            progress.setStatus("Update check skipped: " + ex.getMessage());
        }
    }

    private static String readLatestVersion() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(VERSION_URL).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "CreateWorkOrderProms/" + AppVersion.VERSION);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            String line = reader.readLine();
            if (line == null || line.trim().isEmpty()) {
                throw new IOException("latest version is empty");
            }
            return normalizeVersion(line);
        } finally {
            connection.disconnect();
        }
    }

    private static void startUpdater(String latestVersion) throws IOException {
        File updater = findUpdaterJar();
        if (!updater.exists()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Auto update file was not found:\n" + updater.getAbsolutePath()
                    + "\n\nPlease download CreateWorkOrderProms.zip from GitHub Releases.",
                    "CreateWorkOrderProms Update",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        File tempUpdater = new File(System.getProperty("java.io.tmpdir"), "CreateWorkOrderProms-AutoUpdater-" + System.currentTimeMillis() + ".jar");
        Files.copy(updater.toPath(), tempUpdater.toPath(), StandardCopyOption.REPLACE_EXISTING);

        new ProcessBuilder(getJavaExecutable(), "-jar", tempUpdater.getAbsolutePath(), latestVersion, getProgramFolder().getAbsolutePath())
                .directory(getProgramFolder())
                .start();
    }

    private static File findUpdaterJar() {
        File appFolder = getProgramFolder();
        File updater = new File(appFolder, UPDATE_JAR_PATH);
        if (updater.exists()) {
            return updater;
        }
        return new File(appFolder, UPDATE_JAR_NAME);
    }

    private static String getJavaExecutable() {
        File javaw = new File(new File(System.getProperty("java.home"), "bin"), "javaw.exe");
        if (javaw.exists()) {
            return javaw.getAbsolutePath();
        }
        return new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath();
    }

    private static boolean isNewerVersion(String latestVersion, String currentVersion) {
        int[] latest = parseVersion(latestVersion);
        int[] current = parseVersion(currentVersion);
        int maxLength = Math.max(latest.length, current.length);
        for (int i = 0; i < maxLength; i++) {
            int latestPart = i < latest.length ? latest[i] : 0;
            int currentPart = i < current.length ? current[i] : 0;
            if (latestPart > currentPart) {
                return true;
            }
            if (latestPart < currentPart) {
                return false;
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        String[] parts = normalizeVersion(version).split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ex) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }

    private static String normalizeVersion(String version) {
        if (version == null) {
            return "0";
        }
        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized.replaceAll("[^0-9.].*$", "");
    }

    private static String formatVersion(String version) {
        return "v" + normalizeVersion(version);
    }

    private static File getProgramFolder() {
        return AppPaths.getAppFolder();
    }
}
