package com.java.myapp;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginManager {

    private static final String APP_FOLDER_NAME = "CreateWorkOrderProms";
    private static final String USER_FOLDER_NAME = "User";
    private static final String LOGIN_FILE_NAME = "login.properties";
    private static final String PASSWORD_PREFIX = "v1:";
    private static final byte[] KEY_SALT = "CreateWorkOrderProms.Login.v1".getBytes();

    public static class Credentials {

        private final String username;
        private final String password;

        public Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    public static Credentials loadOrPrompt() throws IOException {
        Credentials credentials = loadCredentials();
        if (credentials != null) {
            return credentials;
        }
        return promptAndSave(null);
    }

    public static Credentials editCredentials() throws IOException {
        return promptAndSave(loadCredentials());
    }

    private static Credentials promptAndSave(Credentials currentCredentials) throws IOException {
        while (true) {
            LoginForm form = new LoginForm(currentCredentials);
            int result = JOptionPane.showConfirmDialog(
                    null,
                    form.getPanel(),
                    "PROMs Login",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) {
                return currentCredentials;
            }

            Credentials credentials = form.getCredentials();
            if (credentials.getUsername().isEmpty() || credentials.getPassword().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter username and password.");
                currentCredentials = credentials;
                continue;
            }

            saveCredentials(credentials);
            return credentials;
        }
    }

    private static Credentials loadCredentials() throws IOException {
        File loginFile = getLoginFile();
        migrateLegacyLoginIfNeeded(loginFile);
        if (!loginFile.exists()) {
            return null;
        }

        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(loginFile)) {
            properties.load(input);
        }

        String username = properties.getProperty("username", "").trim();
        String protectedPassword = properties.getProperty("password", "");
        if (username.isEmpty() || protectedPassword.isEmpty()) {
            return null;
        }

        try {
            return new Credentials(username, decryptPassword(protectedPassword));
        } catch (Exception ex) {
            throw new IOException("Cannot read saved login. Please edit login again.", ex);
        }
    }

    private static void saveCredentials(Credentials credentials) throws IOException {
        File loginFile = getLoginFile();
        File parent = loginFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create login folder: " + parent.getAbsolutePath());
        }

        Properties properties = new Properties();
        properties.setProperty("username", credentials.getUsername());
        try {
            properties.setProperty("password", encryptPassword(credentials.getPassword()));
        } catch (Exception ex) {
            throw new IOException("Cannot save password.", ex);
        }

        try (FileOutputStream output = new FileOutputStream(loginFile)) {
            properties.store(output, "CreateWorkOrderProms login");
        }
    }

    private static File getLoginFile() {
        return new File(getProgramFolder(), USER_FOLDER_NAME + File.separator + LOGIN_FILE_NAME);
    }

    private static File getProgramFolder() {
        return AppPaths.getAppFolder();
    }

    private static File getLegacyLoginFile() {
        String basePath = System.getenv("LOCALAPPDATA");
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = System.getProperty("user.home");
        }
        return new File(new File(basePath, APP_FOLDER_NAME), LOGIN_FILE_NAME);
    }

    private static void migrateLegacyLoginIfNeeded(File loginFile) throws IOException {
        if (loginFile.exists()) {
            return;
        }

        File legacyLoginFile = getLegacyLoginFile();
        if (!legacyLoginFile.exists()) {
            migrateOldLoginTxtIfNeeded();
            return;
        }

        File parent = loginFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create login folder: " + parent.getAbsolutePath());
        }

        Files.move(legacyLoginFile.toPath(), loginFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void migrateOldLoginTxtIfNeeded() throws IOException {
        File oldLoginTextFile = new File(getProgramFolder(), "Login.txt");
        if (!oldLoginTextFile.exists()) {
            return;
        }

        String username = "";
        String password = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(oldLoginTextFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length != 2) {
                    continue;
                }
                if (parts[0].trim().equalsIgnoreCase("User")) {
                    username = parts[1].trim();
                } else if (parts[0].trim().equalsIgnoreCase("Password")) {
                    password = parts[1].trim();
                }
            }
        } catch (IOException ex) {
            System.out.println("Cannot migrate Login.txt: " + ex.toString());
            return;
        }

        if (!username.isEmpty() && !password.isEmpty()) {
            saveCredentials(new Credentials(username, password));
        }
    }

    private static String encryptPassword(String password) throws Exception {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, createKey(), new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(password.getBytes("UTF-8"));

        return PASSWORD_PREFIX
                + Base64.getEncoder().encodeToString(iv)
                + ":"
                + Base64.getEncoder().encodeToString(encrypted);
    }

    private static String decryptPassword(String protectedPassword) throws Exception {
        if (!protectedPassword.startsWith(PASSWORD_PREFIX)) {
            return protectedPassword;
        }

        String[] parts = protectedPassword.substring(PASSWORD_PREFIX.length()).split(":");
        if (parts.length != 2) {
            throw new IOException("Invalid saved password format.");
        }

        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] encrypted = Base64.getDecoder().decode(parts[1]);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, createKey(), new IvParameterSpec(iv));
        return new String(cipher.doFinal(encrypted), "UTF-8");
    }

    private static SecretKeySpec createKey() throws Exception {
        String keyText = System.getProperty("user.name", "")
                + "|"
                + System.getenv("COMPUTERNAME")
                + "|"
                + System.getProperty("user.home", "");
        KeySpec spec = new PBEKeySpec(keyText.toCharArray(), KEY_SALT, 12000, 128);
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (Exception ex) {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        }
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private static class LoginForm {

        private final JPanel panel;
        private final JTextField usernameField;
        private final JPasswordField passwordField;

        LoginForm(Credentials currentCredentials) {
            panel = new JPanel(new GridBagLayout());
            usernameField = new JTextField(22);
            passwordField = new JPasswordField(22);
            passwordField.setEchoChar('*');

            if (currentCredentials != null) {
                usernameField.setText(currentCredentials.getUsername());
                passwordField.setText(currentCredentials.getPassword());
            }

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Username:"), gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(usernameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(new JLabel("Password:"), gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(passwordField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            panel.add(new JLabel("Password is hidden and saved on this computer."), gbc);
        }

        JPanel getPanel() {
            return panel;
        }

        Credentials getCredentials() {
            char[] passwordChars = passwordField.getPassword();
            try {
                return new Credentials(usernameField.getText().trim(), new String(passwordChars));
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        }
    }
}
