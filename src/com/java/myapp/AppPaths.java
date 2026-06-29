package com.java.myapp;

import java.io.File;
import java.io.IOException;

public final class AppPaths {

    private AppPaths() {
    }

    public static File getAppFolder() {
        try {
            File location = new File(AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (location.isFile()) {
                return location.getParentFile().getCanonicalFile();
            }
        } catch (Exception ex) {
            System.out.println("Cannot detect jar folder: " + ex.toString());
        }

        try {
            return new File(".").getCanonicalFile();
        } catch (IOException ex) {
            return new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        }
    }
}
