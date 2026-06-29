package com.java.myapp;

public final class AppVersion {

    public static final String VERSION = "1.0.0";

    private AppVersion() {
    }

    public static String displayVersion() {
        return "v" + VERSION;
    }
}
