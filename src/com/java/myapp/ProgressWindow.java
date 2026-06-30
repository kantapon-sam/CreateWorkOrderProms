package com.java.myapp;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class ProgressWindow extends JFrame {

    private final JLabel statusLabel;
    private final JLabel versionLabel;
    private final JLabel downloadLabel;
    private final JLabel workLabel;
    private final JProgressBar downloadBar;
    private final JProgressBar workBar;
    private final JButton editLoginButton;

    public ProgressWindow() {
        super("CreateWorkOrderProms " + AppVersion.displayVersion() + " Progress");
        versionLabel = new JLabel("CreateWorkOrderProms " + AppVersion.displayVersion());
        versionLabel.setFont(versionLabel.getFont().deriveFont(Font.BOLD));
        statusLabel = new JLabel("Starting...");
        downloadLabel = new JLabel("ChromeDriver download");
        workLabel = new JLabel("Work order progress");
        downloadBar = new JProgressBar(0, 100);
        workBar = new JProgressBar(0, 100);
        editLoginButton = new JButton("Change Login");
        editLoginButton.setEnabled(false);

        downloadBar.setStringPainted(true);
        downloadBar.setString("No download needed");
        workBar.setStringPainted(true);
        workBar.setString("Waiting");

        JPanel panel = new JPanel(new GridLayout(7, 1, 6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(versionLabel);
        panel.add(statusLabel);
        panel.add(downloadLabel);
        panel.add(downloadBar);
        panel.add(workLabel);
        panel.add(workBar);
        panel.add(editLoginButton);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setSize(500, 260);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void showWindow() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setVisible(true);
            }
        });
    }

    public void setStatus(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                statusLabel.setText(message);
            }
        });
    }

    public void setEditLoginAction(final Runnable action) {
        runOnEventThreadAndWait(new Runnable() {
            @Override
            public void run() {
                for (ActionListener listener : editLoginButton.getActionListeners()) {
                    editLoginButton.removeActionListener(listener);
                }
                editLoginButton.setEnabled(action != null);
                if (action != null) {
                    editLoginButton.addActionListener(new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent event) {
                            action.run();
                        }
                    });
                }
            }
        });
    }

    public void setDownloadProgress(final long bytesRead, final long totalBytes) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (totalBytes > 0) {
                    int percent = (int) Math.min(100, (bytesRead * 100) / totalBytes);
                    downloadBar.setIndeterminate(false);
                    downloadBar.setValue(percent);
                    downloadBar.setString(percent + "% (" + formatBytes(bytesRead) + " / " + formatBytes(totalBytes) + ")");
                } else {
                    downloadBar.setIndeterminate(true);
                    downloadBar.setString(formatBytes(bytesRead));
                }
            }
        });
    }

    public void setDownloadStatus(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                downloadBar.setIndeterminate(false);
                downloadBar.setString(message);
            }
        });
    }

    public void setWorkProgress(final int completed, final int total, final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int safeTotal = Math.max(total, 1);
                int safeCompleted = Math.max(0, Math.min(completed, safeTotal));
                int percent = (safeCompleted * 100) / safeTotal;
                workBar.setMaximum(safeTotal);
                workBar.setValue(safeCompleted);
                workLabel.setText(message);
                workBar.setString(safeCompleted + "/" + safeTotal + " (" + percent + "%)");
            }
        });
    }

    public void closeWindow() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dispose();
            }
        });
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }

    private void runOnEventThreadAndWait(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }
}
