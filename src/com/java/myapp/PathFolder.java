package com.java.myapp;

import java.io.*;

public class PathFolder {

    private String current;
    private String Login;
    private String ListWorkOrder;
    private String Chromedriver;
    private String ZipChromedriver;

    public PathFolder() {
        try {
            current = AppPaths.getAppFolder().getCanonicalPath();
            Login = current + "\\Login.txt";
            ListWorkOrder = current + "\\ListWorkOrder.csv";
            Chromedriver = current + "\\chromedriver.exe";
            ZipChromedriver = current + "\\chromedriver-win64.zip";
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public String getCurrent() {
        return current;
    }

    public String getLogin() {
        return Login;
    }

    public String getChromedriver() {
        return Chromedriver;
    }

    public String getZipChromedriver() {
        return ZipChromedriver;
    }

    public String getListWorkOrder() {
        return ListWorkOrder;
    }

   

}
