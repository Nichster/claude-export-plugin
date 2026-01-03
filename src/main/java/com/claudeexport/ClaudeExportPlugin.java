package com.claudeexport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Slf4j
@PluginDescriptor(
    name = "Claude Export",
    description = "Export bank, inventory, equipment, quests, and skills data for Claude AI integration",
    tags = {"claude", "export", "mcp", "ai", "bank"}
)
public class ClaudeExportPlugin extends Plugin {
    
    private static final String EXPORT_FOLDER = "claude-export";
    
    @Inject
    private Client client;
    
    @Inject
    private ClaudeExportConfig config;
    
    @Inject
    private ItemManager itemManager;
    
    @Inject
    private ClientToolbar clientToolbar;
    
    private NavigationButton navButton;
    private ClaudeExportPanel panel;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File exportDir;
    
    @Override
    protected void startUp() throws Exception {
        log.info("Claude Export plugin started");
        
        // Create export directory
        exportDir = new File(net.runelite.client.RuneLite.RUNELITE_DIR, EXPORT_FOLDER);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        
        // Create panel
        panel = new ClaudeExportPanel(this);
        
        // Create navigation button
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/export_icon.png");
        navButton = NavigationButton.builder()
            .tooltip("Claude Export")
            .icon(icon != null ? icon : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB))
            .priority(10)
            .panel(panel)
            .build();
        
        clientToolbar.addNavigation(navButton);
    }
    
    @Override
    protected void shutDown() throws Exception {
        log.info("Claude Export plugin stopped");
        clientToolbar.removeNavigation(navButton);
    }
    
    @Provides
    ClaudeExportConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ClaudeExportConfig.class);
    }
    
    @Subscribe
public void onGameStateChanged(GameStateChanged event) {
    if (event.getGameState() == GameState.LOGGED_IN && config.autoExportOnLogin()) {
        // Export after a short delay to allow data to load
        exportSkills();
        exportQuests();
    }
}
    
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (!config.autoExportOnChange()) {
            return;
        }
        
        int containerId = event.getContainerId();
        
        if (containerId == InventoryID.BANK.getId()) {
            exportBank();
        } else if (containerId == InventoryID.INVENTORY.getId()) {
            exportInventory();
        } else if (containerId == InventoryID.EQUIPMENT.getId()) {
            exportEquipment();
        }
    }
    
    /**
     * Get the player's username
     */
    public String getUsername() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return "unknown";
        }
        return player.getName();
    }
    
    /**
     * Export bank contents to JSON
     */
    public void exportBank() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            log.warn("Cannot export bank - not logged in");
            return;
        }
        
        ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);
        if (bankContainer == null) {
            log.warn("Bank container not available - open your bank first");
            return;
        }
        
        List<Map<String, Object>> items = new ArrayList<>();
        Item[] bankItems = bankContainer.getItems();
        
        for (int i = 0; i < bankItems.length; i++) {
            Item item = bankItems[i];
            if (item.getId() == -1 || item.getQuantity() == 0) {
                continue;
            }
            
            ItemComposition comp = itemManager.getItemComposition(item.getId());
            int price = itemManager.getItemPrice(item.getId());
            
            Map<String, Object> itemData = new LinkedHashMap<>();
            itemData.put("id", item.getId());
            itemData.put("name", comp.getName());
            itemData.put("quantity", item.getQuantity());
            itemData.put("price", price);
            itemData.put("tab", getBankTab(i, bankItems.length));
            
            items.add(itemData);
        }
        
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("username", getUsername());
        export.put("timestamp", Instant.now().toString());
        export.put("items", items);
        
        writeJson("bank.json", export);
        log.info("Exported {} bank items", items.size());
    }
    
    /**
     * Export inventory contents to JSON
     */
    public void exportInventory() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            log.warn("Cannot export inventory - not logged in");
            return;
        }
        
        ItemContainer invContainer = client.getItemContainer(InventoryID.INVENTORY);
        if (invContainer == null) {
            return;
        }
        
        List<Map<String, Object>> items = new ArrayList<>();
        Item[] invItems = invContainer.getItems();
        
        for (int i = 0; i < 28; i++) {
            if (i < invItems.length && invItems[i].getId() != -1) {
                Item item = invItems[i];
                ItemComposition comp = itemManager.getItemComposition(item.getId());
                int price = itemManager.getItemPrice(item.getId());
                
                Map<String, Object> itemData = new LinkedHashMap<>();
                itemData.put("slot", i);
                itemData.put("id", item.getId());
                itemData.put("name", comp.getName());
                itemData.put("quantity", item.getQuantity());
                itemData.put("price", price);
                
                items.add(itemData);
            } else {
                items.add(null);
            }
        }
        
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("username", getUsername());
        export.put("timestamp", Instant.now().toString());
        export.put("items", items);
        
        writeJson("inventory.json", export);
        log.info("Exported inventory");
    }
    
    /**
     * Export equipment to JSON
     */
    public void exportEquipment() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            log.warn("Cannot export equipment - not logged in");
            return;
        }
        
        ItemContainer equipContainer = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipContainer == null) {
            return;
        }
        
        String[] slotNames = {
            "head", "cape", "neck", "weapon", "body",
            "shield", "legs", "hands", "feet", "ring", "ammo"
        };
        
        int[] slotIds = {
            EquipmentInventorySlot.HEAD.getSlotIdx(),
            EquipmentInventorySlot.CAPE.getSlotIdx(),
            EquipmentInventorySlot.AMULET.getSlotIdx(),
            EquipmentInventorySlot.WEAPON.getSlotIdx(),
            EquipmentInventorySlot.BODY.getSlotIdx(),
            EquipmentInventorySlot.SHIELD.getSlotIdx(),
            EquipmentInventorySlot.LEGS.getSlotIdx(),
            EquipmentInventorySlot.GLOVES.getSlotIdx(),
            EquipmentInventorySlot.BOOTS.getSlotIdx(),
            EquipmentInventorySlot.RING.getSlotIdx(),
            EquipmentInventorySlot.AMMO.getSlotIdx()
        };
        
        Map<String, Object> slots = new LinkedHashMap<>();
        Item[] items = equipContainer.getItems();
        
        for (int i = 0; i < slotNames.length && i < slotIds.length; i++) {
            int slotIdx = slotIds[i];
            if (slotIdx < items.length && items[slotIdx].getId() != -1) {
                Item item = items[slotIdx];
                ItemComposition comp = itemManager.getItemComposition(item.getId());
                int price = itemManager.getItemPrice(item.getId());
                
                Map<String, Object> itemData = new LinkedHashMap<>();
                itemData.put("id", item.getId());
                itemData.put("name", comp.getName());
                itemData.put("quantity", item.getQuantity());
                itemData.put("price", price);
                
                slots.put(slotNames[i], itemData);
            } else {
                slots.put(slotNames[i], null);
            }
        }
        
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("username", getUsername());
        export.put("timestamp", Instant.now().toString());
        export.put("slots", slots);
        
        writeJson("equipment.json", export);
        log.info("Exported equipment");
    }
    
    /**
     * Export quest statuses to JSON
     */
    public void exportQuests() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            log.warn("Cannot export quests - not logged in");
            return;
        }
        
        Map<String, String> quests = new LinkedHashMap<>();
        
        for (Quest quest : Quest.values()) {
            QuestState state = quest.getState(client);
            quests.put(quest.getName(), state.name());
        }
        
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("username", getUsername());
        export.put("timestamp", Instant.now().toString());
        export.put("quests", quests);
        
        writeJson("quests.json", export);
        log.info("Exported {} quests", quests.size());
    }
    
    /**
     * Export skills to JSON
     */
    public void exportSkills() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            log.warn("Cannot export skills - not logged in");
            return;
        }
        
        Map<String, Object> skills = new LinkedHashMap<>();
        
        for (Skill skill : Skill.values()) {
            Map<String, Object> skillData = new LinkedHashMap<>();
            skillData.put("level", client.getRealSkillLevel(skill));
            skillData.put("xp", client.getSkillExperience(skill));
            skillData.put("boostedLevel", client.getBoostedSkillLevel(skill));
            
            skills.put(skill.getName(), skillData);
        }
        
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("username", getUsername());
        export.put("timestamp", Instant.now().toString());
        export.put("skills", skills);
        
        writeJson("skills.json", export);
        log.info("Exported skills");
    }
    
    /**
     * Export all data
     */
    public void exportAll() {
        exportBank();
        exportInventory();
        exportEquipment();
        exportQuests();
        exportSkills();
        log.info("Exported all data for Claude MCP");
    }
    
    /**
     * Get the bank tab for an item index (simplified - requires tab separator tracking)
     */
    private int getBankTab(int index, int totalItems) {
        // Simplified implementation - in production you'd track tab separators
        // RuneLite's BankPlugin can provide this info
        return 0;
    }
    
    /**
     * Write data to JSON file
     */
    private void writeJson(String filename, Object data) {
        File file = new File(exportDir, filename);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            log.error("Failed to write {}: {}", filename, e.getMessage());
        }
    }
    
    /**
     * Get the export directory path
     */
    public String getExportPath() {
        return exportDir.getAbsolutePath();
    }
}
