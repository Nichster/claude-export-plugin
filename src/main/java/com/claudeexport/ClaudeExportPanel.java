package com.claudeexport;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.net.URI;

public class ClaudeExportPanel extends PluginPanel {

    private final ClaudeExportPlugin plugin;
    private JLabel statusLabel;
    private JLabel lastExportLabel;
    private Timer statusResetTimer;

    // Colors
    private static final Color CLAUDE_ORANGE = new Color(217, 119, 87);
    private static final Color CLAUDE_DARK = new Color(41, 37, 36);
    private static final Color SUCCESS_GREEN = new Color(76, 175, 80);

    public ClaudeExportPanel(ClaudeExportPlugin plugin) {
        super(false);
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Main container with scroll
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        mainContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header section
        mainContainer.add(createHeaderSection());
        mainContainer.add(Box.createRigidArea(new Dimension(0, 15)));

        // Claude AI section (the main differentiator)
        mainContainer.add(createClaudeSection());
        mainContainer.add(Box.createRigidArea(new Dimension(0, 15)));

        // Export buttons section
        mainContainer.add(createExportSection());
        mainContainer.add(Box.createRigidArea(new Dimension(0, 15)));

        // File management section
        mainContainer.add(createFileSection());
        mainContainer.add(Box.createRigidArea(new Dimension(0, 15)));

        // MCP Info section
        mainContainer.add(createMcpSection());
        mainContainer.add(Box.createRigidArea(new Dimension(0, 15)));

        // Status section
        JPanel statusSection = createStatusSection();
        mainContainer.add(statusSection);

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(mainContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel titleLabel = new JLabel("Claude Export");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(titleLabel, BorderLayout.NORTH);

        JLabel descLabel = new JLabel("<html>Export OSRS data in Claude AI-optimized formats.</html>");
        descLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        panel.add(descLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createClaudeSection() {
        JPanel panel = createSectionPanel("Claude AI Integration");

        // Copy for Claude button (main feature)
        JButton copyForClaudeBtn = createPrimaryButton("Copy for Claude", e -> {
            String summary = plugin.generateClaudeSummary();
            copyToClipboard(summary);
            showSuccess("Copied to clipboard!");
        });
        copyForClaudeBtn.setToolTipText("Copy a formatted account summary to paste into Claude");
        panel.add(copyForClaudeBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Generate Prompt button
        JButton generatePromptBtn = createPrimaryButton("Generate Claude Prompt", e -> {
            String prompt = plugin.generateClaudePrompt();
            copyToClipboard(prompt);
            showSuccess("Prompt copied to clipboard!");
        });
        generatePromptBtn.setToolTipText("Generate a starter prompt with your data for Claude");
        panel.add(generatePromptBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Export Claude Context button
        JButton exportContextBtn = createSecondaryButton("Export Claude Context", e -> {
            plugin.exportClaudeContext();
            showSuccess("Exported claude-context.json!");
        });
        exportContextBtn.setToolTipText("Export combined JSON file with metadata for MCP");
        panel.add(exportContextBtn);

        return panel;
    }

    private JPanel createExportSection() {
        JPanel panel = createSectionPanel("Export Data");

        // Export All button
        JButton exportAllBtn = createSecondaryButton("Export All Data", e -> {
            plugin.exportAll();
            showSuccess("Exported all data!");
        });
        panel.add(exportAllBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Individual export buttons in a grid
        JPanel gridPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        gridPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        gridPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        gridPanel.add(createSmallButton("Skills", e -> {
            plugin.exportSkills();
            showSuccess("Exported skills!");
        }));
        gridPanel.add(createSmallButton("Quests", e -> {
            plugin.exportQuests();
            showSuccess("Exported quests!");
        }));
        gridPanel.add(createSmallButton("Bank", e -> {
            plugin.exportBank();
            showSuccess("Exported bank!");
        }));
        gridPanel.add(createSmallButton("Inventory", e -> {
            plugin.exportInventory();
            showSuccess("Exported inventory!");
        }));
        gridPanel.add(createSmallButton("Equipment", e -> {
            plugin.exportEquipment();
            showSuccess("Exported equipment!");
        }));

        panel.add(gridPanel);

        return panel;
    }

    private JPanel createFileSection() {
        JPanel panel = createSectionPanel("Files");

        // Open Folder button
        JButton openFolderBtn = createSecondaryButton("Open Export Folder", e -> {
            try {
                Desktop.getDesktop().open(new File(plugin.getExportPath()));
            } catch (Exception ex) {
                showError("Could not open folder");
            }
        });
        panel.add(openFolderBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Copy Path button
        JButton copyPathBtn = createSecondaryButton("Copy Folder Path", e -> {
            copyToClipboard(plugin.getExportPath());
            showSuccess("Path copied!");
        });
        panel.add(copyPathBtn);

        return panel;
    }

    private JPanel createMcpSection() {
        JPanel panel = createSectionPanel("MCP Integration");

        JLabel infoLabel = new JLabel("<html><small>This plugin works with Claude MCP Server for real-time Claude Desktop integration.</small></html>");
        infoLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(infoLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // MCP Server link button
        JButton mcpLinkBtn = createLinkButton("View MCP Server Setup", e -> {
            try {
                Desktop.getDesktop().browse(new URI(plugin.getMcpServerUrl()));
            } catch (Exception ex) {
                copyToClipboard(plugin.getMcpServerUrl());
                showSuccess("URL copied to clipboard!");
            }
        });
        panel.add(mcpLinkBtn);

        return panel;
    }

    private JPanel createStatusSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Status label
        statusLabel = new JLabel("Ready to export");
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusLabel);

        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Last export label
        lastExportLabel = new JLabel("Last export: Never");
        lastExportLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        lastExportLabel.setFont(lastExportLabel.getFont().deriveFont(Font.PLAIN, 11f));
        lastExportLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lastExportLabel);

        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Path label
        JLabel pathLabel = new JLabel("<html><small>Path: ~/.runelite/claude-export/</small></html>");
        pathLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        pathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(pathLabel);

        return panel;
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(CLAUDE_ORANGE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        panel.add(titleLabel);

        return panel;
    }

    private JButton createPrimaryButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        button.setBackground(CLAUDE_ORANGE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.addActionListener(listener);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createSecondaryButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.addActionListener(listener);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createSmallButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(11f));
        button.addActionListener(listener);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createLinkButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton("<html><u>" + text + "</u></html>");
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        button.setBackground(ColorScheme.DARK_GRAY_COLOR);
        button.setForeground(new Color(100, 149, 237)); // Cornflower blue
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(listener);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
            new StringSelection(text), null
        );
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(SUCCESS_GREEN);
        resetStatusAfterDelay();
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(new Color(244, 67, 54)); // Red
        resetStatusAfterDelay();
    }

    private void resetStatusAfterDelay() {
        if (statusResetTimer != null) {
            statusResetTimer.stop();
        }
        statusResetTimer = new Timer(3000, e -> {
            statusLabel.setText("Ready to export");
            statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        });
        statusResetTimer.setRepeats(false);
        statusResetTimer.start();
    }

    /**
     * Update the last export label - called from plugin
     */
    public void updateLastExportLabel() {
        SwingUtilities.invokeLater(() -> {
            lastExportLabel.setText("Last export: " + plugin.getFormattedLastExportTime());
        });
    }
}
