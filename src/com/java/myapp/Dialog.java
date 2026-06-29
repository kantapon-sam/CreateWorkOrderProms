package com.java.myapp;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class Dialog {

    private int lineNumber;
    private String name_file;

    public Dialog() {
    }

    public static void setLAF() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    public static void NewProgram() {
        JOptionPane.showMessageDialog(null, "Please start a new program.");
    }

    public static void Success() {
        JOptionPane.showMessageDialog(null, "Success", "CreateWorkOrderProms " + AppVersion.displayVersion(), JOptionPane.INFORMATION_MESSAGE);
    }

    public void GoogleChrome() {
        JOptionPane.showMessageDialog(null, "Please install Google Chrome.");
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getName_file() {
        return name_file;
    }

    public void setName_file(String name_file) {
        this.name_file = name_file;
    }
}
