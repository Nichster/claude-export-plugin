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

    public ClaudeExportPanel(ClaudeExportPlugin plugin) {
        super(false);
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title
        JLabel title = new JLabel("Claude Export");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(title);

        JLabel subtitle = new JLabel("Export OSRS data for Claude AI");
        subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        subtitle.setFont(subtitle.getFont().deriveFont(11f));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(subtitle);
        mainPanel.add(Box.createVerticalStrut(15));

        // Claude AI Section
        mainPanel.add(createSectionHeader("CLAUDE AI"));
        mainPanel.add(Box.createVerticalStrut(5));

        JButton copyBtn = createButton("Copy for Claude", ColorScheme.BRAND_ORANGE);
        copyBtn.setToolTipText("<html>Copy a formatted summary of your account<br>optimized for pasting into Claude AI conversations</html>");
        copyBtn.addActionListener(e -> {
            String text = plugin.generateClaudeSummary();
            copyToClipboard(text);
            showButtonFeedback(copyBtn, "Copied!");
        });
        mainPanel.add(copyBtn);
        mainPanel.add(Box.createVerticalStrut(5));

        JButton promptBtn = createButton("Generate Prompt", ColorScheme.BRAND_ORANGE);
        promptBtn.setToolTipText("<html>Copy a starter prompt with your account data<br>that you can paste directly into Claude</html>");
        promptBtn.addActionListener(e -> {
            String text = plugin.generateClaudePrompt();
            copyToClipboard(text);
            showButtonFeedback(promptBtn, "Copied!");
        });
        mainPanel.add(promptBtn);
        mainPanel.add(Box.createVerticalStrut(15));

        // Export Data Section
        mainPanel.add(createSectionHeader("EXPORT DATA"));
        mainPanel.add(Box.createVerticalStrut(5));

        JButton exportAllBtn = createButton("Export All", ColorScheme.DARKER_GRAY_COLOR);
        exportAllBtn.setToolTipText("Export all data (skills, quests, bank, inventory, equipment) to JSON files");
        exportAllBtn.addActionListener(e -> {
            plugin.exportAll();
            showButtonFeedback(exportAllBtn, "Exported!");
        });
        mainPanel.add(exportAllBtn);
        mainPanel.add(Box.createVerticalStrut(5));

        JButton contextBtn = createButton("Export Context", ColorScheme.DARKER_GRAY_COLOR);
        contextBtn.setToolTipText("Export a combined claude-context.json file with all data and metadata");
        contextBtn.addActionListener(e -> {
            plugin.exportClaudeContext();
            showButtonFeedback(contextBtn, "Exported!");
        });
        mainPanel.add(contextBtn);
        mainPanel.add(Box.createVerticalStrut(8));

        // Individual export buttons - row 1
        JPanel row1 = new JPanel(new GridLayout(1, 3, 4, 0));
        row1.setBackground(ColorScheme.DARK_GRAY_COLOR);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton skillsBtn = createSmallButton("Skills", e -> { plugin.exportSkills(); updateStatus("Skills exported"); });
        skillsBtn.setToolTipText("Export skill levels and XP only");
        row1.add(skillsBtn);

        JButton questsBtn = createSmallButton("Quests", e -> { plugin.exportQuests(); updateStatus("Quests exported"); });
        questsBtn.setToolTipText("Export quest progress only");
        row1.add(questsBtn);

        JButton bankBtn = createSmallButton("Bank", e -> { plugin.exportBank(); updateStatus("Bank exported"); });
        bankBtn.setToolTipText("Export bank contents (must have bank open)");
        row1.add(bankBtn);

        mainPanel.add(row1);
        mainPanel.add(Box.createVerticalStrut(4));

        // Individual export buttons - row 2
        JPanel row2 = new JPanel(new GridLayout(1, 3, 4, 0));
        row2.setBackground(ColorScheme.DARK_GRAY_COLOR);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton invBtn = createSmallButton("Inventory", e -> { plugin.exportInventory(); updateStatus("Inventory exported"); });
        invBtn.setToolTipText("Export current inventory only");
        row2.add(invBtn);

        JButton equipBtn = createSmallButton("Equipment", e -> { plugin.exportEquipment(); updateStatus("Equipment exported"); });
        equipBtn.setToolTipText("Export equipped items only");
        row2.add(equipBtn);

        row2.add(Box.createHorizontalGlue());
        mainPanel.add(row2);
        mainPanel.add(Box.createVerticalStrut(15));

        // Files Section
        mainPanel.add(createSectionHeader("FILES"));
        mainPanel.add(Box.createVerticalStrut(5));

        JButton openBtn = createButton("Open Folder", ColorScheme.DARKER_GRAY_COLOR);
        openBtn.setToolTipText("Open the export folder in your file browser");
        openBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(plugin.getExportPath()));
            } catch (Exception ex) {
                updateStatus("Cannot open folder");
            }
        });
        mainPanel.add(openBtn);
        mainPanel.add(Box.createVerticalStrut(5));

        JButton pathBtn = createButton("Copy Path", ColorScheme.DARKER_GRAY_COLOR);
        pathBtn.setToolTipText("Copy the export folder path to clipboard");
        pathBtn.addActionListener(e -> {
            copyToClipboard(plugin.getExportPath());
            showButtonFeedback(pathBtn, "Copied!");
        });
        mainPanel.add(pathBtn);
        mainPanel.add(Box.createVerticalStrut(15));

        // MCP Integration Section
        mainPanel.add(createSectionHeader("MCP INTEGRATION"));
        mainPanel.add(Box.createVerticalStrut(5));

        JLabel mcpDesc = new JLabel("<html><body style='width: 190px'>" +
            "Connect Claude Desktop to your OSRS data in real-time using MCP." +
            "</body></html>");
        mcpDesc.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        mcpDesc.setFont(mcpDesc.getFont().deriveFont(11f));
        mcpDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(mcpDesc);
        mainPanel.add(Box.createVerticalStrut(5));

        JButton setupBtn = createButton("View Setup Guide", ColorScheme.DARKER_GRAY_COLOR);
        setupBtn.setToolTipText("Open MCP server setup instructions on GitHub");
        setupBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/Nichster/claude-export-plugin/blob/master/MCP_SETUP.md"));
            } catch (Exception ex) {
                copyToClipboard("https://github.com/Nichster/claude-export-plugin/blob/master/MCP_SETUP.md");
                showButtonFeedback(setupBtn, "URL Copied!");
            }
        });
        mainPanel.add(setupBtn);
        mainPanel.add(Box.createVerticalStrut(15));

        // Status Section
        mainPanel.add(createSectionHeader("STATUS"));
        mainPanel.add(Box.createVerticalStrut(5));

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        statusPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.add(statusLabel);

        lastExportLabel = new JLabel("Last export: Never");
        lastExportLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        lastExportLabel.setFont(lastExportLabel.getFont().deriveFont(10f));
        lastExportLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.add(lastExportLabel);

        JLabel pathLabel = new JLabel("~/.runelite/claude-export/");
        pathLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
        pathLabel.setFont(pathLabel.getFont().deriveFont(10f));
        pathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.add(pathLabel);

        mainPanel.add(statusPanel);

        // Scroll pane
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JLabel createSectionHeader(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ColorScheme.BRAND_ORANGE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createSmallButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(10f));
        button.setMargin(new Insets(2, 4, 2, 4));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(listener);
        return button;
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
            new StringSelection(text), null
        );
    }

    private void showButtonFeedback(JButton button, String message) {
        String original = button.getText();
        Color originalBg = button.getBackground();

        button.setText(message);
        button.setBackground(new Color(76, 175, 80));
        button.setEnabled(false);

        Timer timer = new Timer(1200, e -> {
            button.setText(original);
            button.setBackground(originalBg);
            button.setEnabled(true);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        Timer timer = new Timer(2000, e -> statusLabel.setText("Ready"));
        timer.setRepeats(false);
        timer.start();
    }

    public void updateLastExportLabel() {
        SwingUtilities.invokeLater(() -> {
            lastExportLabel.setText("Last export: " + plugin.getFormattedLastExportTime());
        });
    }
}
