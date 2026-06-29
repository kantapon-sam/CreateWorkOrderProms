package com.java.myapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Download_ChromeDriver {

    private static final String CHROME_FOR_TESTING_URL = "https://googlechromelabs.github.io/chrome-for-testing/known-good-versions-with-downloads.json";
    private static final String DRIVER_ZIP_NAME = "chromedriver-win64.zip";
    private static final String DRIVER_FOLDER_NAME = "chromedriver-win64";
    private static final String DRIVER_FILE_NAME = "chromedriver.exe";

    public interface ProgressListener {

        void onStatus(String message);

        void onProgress(long bytesRead, long totalBytes);
    }

    public void ensureChromeDriver(String destinationFolder, String chromedriverPath, ProgressListener listener) throws IOException {
        notifyStatus(listener, "Checking Chrome version...");
        String chromeVersion = getChromeVersion();
        if (chromeVersion == null || chromeVersion.contains("not found")) {
            throw new IOException("Chrome version not found");
        }

        String chromeMajorVersion = getMajorVersion(chromeVersion);
        String driverVersion = getChromeDriverVersion(chromedriverPath);
        if (driverVersion != null && chromeMajorVersion.equals(getMajorVersion(driverVersion))) {
            notifyStatus(listener, "ChromeDriver is ready: " + driverVersion);
            return;
        }

        notifyStatus(listener, "Updating ChromeDriver for Chrome " + chromeVersion + "...");
        String jsonString = readJsonFromUrl(CHROME_FOR_TESTING_URL);
        String zipFileUrl = findWin64ChromeDriverUrl(jsonString, chromeMajorVersion);

        downloadZipFile(zipFileUrl, destinationFolder, listener);
        notifyStatus(listener, "Extracting ChromeDriver...");
        unzip(destinationFolder + File.separator + DRIVER_ZIP_NAME, destinationFolder);
        moveFile(destinationFolder + File.separator + DRIVER_FOLDER_NAME + File.separator, destinationFolder, DRIVER_FILE_NAME);

        deleteFolder(new File(destinationFolder + File.separator + DRIVER_FOLDER_NAME));
        File zipFile = new File(destinationFolder + File.separator + DRIVER_ZIP_NAME);
        if (zipFile.exists()) {
            zipFile.delete();
        }
        notifyStatus(listener, "ChromeDriver updated.");
    }

    public String getChromeVersion() throws IOException {
        String version = getChromeVersionFromRegistry("HKEY_CURRENT_USER\\Software\\Google\\Chrome\\BLBeacon");
        if (version != null) {
            return version;
        }
        version = getChromeVersionFromRegistry("HKEY_LOCAL_MACHINE\\Software\\Google\\Chrome\\BLBeacon");
        if (version != null) {
            return version;
        }
        return "Version not found";
    }

    private String getChromeVersionFromRegistry(String registryPath) throws IOException {
        String command = "reg query \"" + registryPath + "\" /v version";
        Process process = Runtime.getRuntime().exec(command);

        InputStream inputStream = process.getInputStream();
        Scanner scanner = new Scanner(inputStream);
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("version")) {
                    return line.split("\\s+")[line.split("\\s+").length - 1];
                }
            }
        } finally {
            scanner.close();
        }
        return null;
    }

    public String readJsonFromUrl(String urlString) throws IOException {
        StringBuilder content = new StringBuilder();

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } finally {
            connection.disconnect();
        }
        return content.toString();
    }

    public void downloadZipFile(String zipFileUrl, String destinationFolder) throws IOException {
        downloadZipFile(zipFileUrl, destinationFolder, null);
    }

    public void downloadZipFile(String zipFileUrl, String destinationFolder, ProgressListener listener) throws IOException {
        notifyStatus(listener, "Downloading ChromeDriver...");
        URL url = new URL(zipFileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        long totalBytes = connection.getContentLengthLong();
        long bytesDownloaded = 0;

        try (InputStream in = connection.getInputStream();
                FileOutputStream out = new FileOutputStream(destinationFolder + File.separator + DRIVER_ZIP_NAME)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesDownloaded += bytesRead;
                notifyProgress(listener, bytesDownloaded, totalBytes);
            }
        } finally {
            connection.disconnect();
        }
    }

    public void unzip(String zipFilePath, String destinationFolder) throws IOException {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                String fileName = zipEntry.getName();
                File newFile = new File(destinationFolder + File.separator + fileName);
                // Create all non-existent parent directories
                newFile.getParentFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    public void moveFile(String sourceFolderPath, String destinationFolderPath, String fileName) throws IOException {
        Path sourcePath = Paths.get(sourceFolderPath, fileName);
        Path destinationPath = Paths.get(destinationFolderPath, fileName);

        Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        folder.delete();
    }

    private String getChromeDriverVersion(String chromedriverPath) throws IOException {
        File driverFile = new File(chromedriverPath);
        if (!driverFile.exists()) {
            return null;
        }

        Process process = new ProcessBuilder(chromedriverPath, "--version").redirectErrorStream(true).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("ChromeDriver ")) {
                return null;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                return null;
            }
            return parts[1];
        }
    }

    private String getMajorVersion(String version) throws IOException {
        if (version == null || version.trim().isEmpty()) {
            throw new IOException("Invalid version: " + version);
        }
        return version.split("\\.")[0];
    }

    private String findWin64ChromeDriverUrl(String jsonString, String chromeMajorVersion) throws IOException {
        Pattern pattern = Pattern.compile(
                "\"version\"\\s*:\\s*\"(" + Pattern.quote(chromeMajorVersion) + "\\.[^\"]+)\"(?:(?!\"version\"\\s*:).)*?\"platform\"\\s*:\\s*\"win64\"\\s*,\\s*\"url\"\\s*:\\s*\"([^\"]*chromedriver-win64\\.zip)\"",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jsonString);
        String zipFileUrl = null;
        while (matcher.find()) {
            zipFileUrl = matcher.group(2).replace("\\/", "/");
        }

        if (zipFileUrl == null) {
            throw new IOException("ChromeDriver download URL not found for Chrome major version " + chromeMajorVersion);
        }
        return zipFileUrl;
    }

    private void notifyStatus(ProgressListener listener, String message) {
        if (listener != null) {
            listener.onStatus(message);
        }
    }

    private void notifyProgress(ProgressListener listener, long bytesRead, long totalBytes) {
        if (listener != null) {
            listener.onProgress(bytesRead, totalBytes);
        }
    }
}
