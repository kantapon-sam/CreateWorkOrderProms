package com.java.myapp;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class WorkOrderTemplateSelection {

    public static final String PROJECT_TYPE_ACC_MPLS = "ACC MPLS";
    public static final String PROJECT_TYPE_BROADCAST = "BROADCAST";
    public static final String COMPANY_TRUE_INTERNET = "TRUE INTERNET CORPORATION CO., LTD.";
    public static final String COMPANY_TRUE_MOVE_H = "TRUE MOVE H UNIVERSAL COMMUNICATION CO., LTD.";

    public static Options prompt() {
        JToggleButton accMplsButton = new JToggleButton(PROJECT_TYPE_ACC_MPLS);
        JToggleButton broadcastButton = new JToggleButton(PROJECT_TYPE_BROADCAST);
        JToggleButton trueInternetButton = new JToggleButton(COMPANY_TRUE_INTERNET);
        JToggleButton trueMoveButton = new JToggleButton(COMPANY_TRUE_MOVE_H);

        accMplsButton.setSelected(true);
        trueMoveButton.setSelected(true);

        ButtonGroup projectTypeGroup = new ButtonGroup();
        projectTypeGroup.add(accMplsButton);
        projectTypeGroup.add(broadcastButton);

        ButtonGroup companyGroup = new ButtonGroup();
        companyGroup.add(trueInternetButton);
        companyGroup.add(trueMoveButton);

        JPanel projectTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        projectTypePanel.setBorder(BorderFactory.createTitledBorder("Project Type"));
        projectTypePanel.add(accMplsButton);
        projectTypePanel.add(broadcastButton);

        JPanel companyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        companyPanel.setBorder(BorderFactory.createTitledBorder("Company"));
        companyPanel.add(trueInternetButton);
        companyPanel.add(trueMoveButton);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        content.add(new JLabel("Select options for Get Work Order Template"), BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new BorderLayout(0, 8));
        optionsPanel.add(projectTypePanel, BorderLayout.NORTH);
        optionsPanel.add(companyPanel, BorderLayout.CENTER);
        content.add(optionsPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                null,
                content,
                "Get Work Order Template",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        String projectType = accMplsButton.isSelected() ? PROJECT_TYPE_ACC_MPLS : PROJECT_TYPE_BROADCAST;
        String company = trueInternetButton.isSelected() ? COMPANY_TRUE_INTERNET : COMPANY_TRUE_MOVE_H;
        return new Options(projectType, company);
    }

    public static class Options {

        private final String projectType;
        private final String company;

        private Options(String projectType, String company) {
            this.projectType = projectType;
            this.company = company;
        }

        public String getProjectType() {
            return projectType;
        }

        public String getCompany() {
            return company;
        }
    }
}
