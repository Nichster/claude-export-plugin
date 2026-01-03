package com.claudeexport;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ClaudeExportPanel extends PluginPanel {
    
    private final ClaudeExportPlugin plugin;
    private final JLabel statusLabel;
    private final JLabel pathLabel;
    
    public ClaudeExportPanel(ClaudeExportPlugin plugin) {
        super(false);
        this.plugin = plugin;
        
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        
        JLabel titleLabel = new JLabel("Claude Export");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel descLabel = new JLabel("<html>Export your OSRS data for use with Claude AI via MCP.</html>");
        descLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descLabel.setBorder(new EmptyBorder(5, 0, 10, 0));
        headerPanel.add(descLabel, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        
        // Export All button
        JButton exportAllBtn = createButton("Export All Data", e -> {
            plugin.exportAll();
            updateStatus("Exported all data!");
        });
        buttonsPanel.add(exportAllBtn);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Individual export buttons
        JButton exportBankBtn = createButton("Export Bank", e -> {
            plugin.exportBank();
            updateStatus("Exported bank!");
        });
        buttonsPanel.add(exportBankBtn);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        JButton exportInvBtn = createButton("Export Inventory", e -> {
            plugin.exportInventory();
            updateStatus("Exported inventory!");
        });
        buttonsPanel.add(exportInvBtn);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        JButton exportEquipBtn = createButton("Export Equipment", e -> {
            plugin.exportEquipment();
            updateStatus("Exported equipment!");
        });
        buttonsPanel.add(exportEquipBtn);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        JButton exportQuestsBtn = createButton("Export Quests", e -> {
            plugin.exportQuests();
            updateStatus("Exported quests!");
        });
        buttonsPanel.add(exportQuestsBtn);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        JButton exportSkillsBtn = createButton("Export Skills", e -> {
            plugin.exportSkills();
            updateStatus("Exported skills!");
        });
        buttonsPanel.add(exportSkillsBtn);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Open folder button
        JButton openFolderBtn = createButton("Open Export Folder", e -> {
            try {
                Desktop.getDesktop().open(new File(plugin.getExportPath()));
            } catch (Exception ex) {
                updateStatus("Could not open folder");
            }
        });
        openFolderBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        buttonsPanel.add(openFolderBtn);
        
        add(buttonsPanel, BorderLayout.CENTER);
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        statusPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        
        statusLabel = new JLabel("Ready to export");
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusPanel.add(statusLabel, BorderLayout.NORTH);
        
        pathLabel = new JLabel("<html><small>Path: ~/.runelite/claude-export/</small></html>");
        pathLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        pathLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        statusPanel.add(pathLabel, BorderLayout.SOUTH);
        
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JButton createButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        button.setBackground(ColorScheme.BRAND_ORANGE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.addActionListener(listener);
        return button;
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
        // Reset after a few seconds
        Timer timer = new Timer(3000, e -> statusLabel.setText("Ready to export"));
        timer.setRepeats(false);
        timer.start();
    }
}
