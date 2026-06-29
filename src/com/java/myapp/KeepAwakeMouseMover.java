package com.java.myapp;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.util.Timer;
import java.util.TimerTask;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

public final class KeepAwakeMouseMover {

    private static final long MOVE_INTERVAL_MS = 30000L;

    private KeepAwakeMouseMover() {
    }

    public static Timer start(final WebDriver driver) {
        final Robot robot;
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            System.out.println("Cannot start mouse keep-awake: " + ex.toString());
            return null;
        } catch (SecurityException ex) {
            System.out.println("Cannot start mouse keep-awake: " + ex.toString());
            return null;
        }

        final Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (driver.getWindowHandle() == null) {
                        return;
                    }
                    PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                    if (pointerInfo == null) {
                        return;
                    }
                    Point location = pointerInfo.getLocation();
                    int newX = location.x > 0 ? location.x - 1 : location.x + 1;
                    robot.mouseMove(newX, location.y);
                    robot.mouseMove(location.x, location.y);
                } catch (NoSuchWindowException ex) {
                    timer.cancel();
                } catch (WebDriverException ex) {
                    timer.cancel();
                } catch (RuntimeException ex) {
                    System.out.println("Mouse keep-awake stopped: " + ex.toString());
                    timer.cancel();
                }
            }
        }, MOVE_INTERVAL_MS, MOVE_INTERVAL_MS);
        return timer;
    }

    public static void stop(Timer timer) {
        if (timer != null) {
            timer.cancel();
        }
    }
}
