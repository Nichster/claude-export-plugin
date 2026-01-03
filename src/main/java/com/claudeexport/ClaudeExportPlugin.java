package com.claudeexport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import lombok.Getter;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@PluginDescriptor(
    name = "Claude Export",
    description = "Export OSRS data in Claude AI-optimized formats for MCP integration",
    tags = {"claude", "export", "mcp", "ai", "bank", "llm", "anthropic"}
)
public class ClaudeExportPlugin extends Plugin {

    private static final String EXPORT_FOLDER = "claude-export";
    private static final String PLUGIN_VERSION = "1.1.0";

    @Inject
    private Client client;

    @Inject
    private ClaudeExportConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ClientToolbar clientToolbar;

    private NavigationButton navButton;
    @Getter
    private ClaudeExportPanel panel;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File exportDir;
    private int loginTickCounter = -1;
    private int intervalTickCounter = 0;
    private static final int LOGIN_DELAY_TICKS = 5;
    private static final int TICKS_PER_MINUTE = 100; // ~0.6s per tick

    @Getter
    private Instant lastExportTime;

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

        // Create navigation button with custom icon
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/claude_icon.png");
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
            // Start countdown to export after quest data loads
            loginTickCounter = LOGIN_DELAY_TICKS;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        // Handle login delay export
        if (loginTickCounter > 0) {
            loginTickCounter--;
        } else if (loginTickCounter == 0) {
            loginTickCounter = -1;
            exportSkills();
            exportQuests();
            if (config.exportClaudeContext()) {
                exportClaudeContext();
            }
            log.info("Auto-exported skills and quests after login");
        }

        // Handle interval export
        if (config.autoExportInterval() > 0 && client.getGameState() == GameState.LOGGED_IN) {
            intervalTickCounter++;
            int intervalTicks = config.autoExportInterval() * TICKS_PER_MINUTE;
            if (intervalTickCounter >= intervalTicks) {
                intervalTickCounter = 0;
                exportAll();
                log.info("Auto-exported all data (interval)");
            }
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
     * Calculate combat level
     */
    public int getCombatLevel() {
        int attack = client.getRealSkillLevel(Skill.ATTACK);
        int strength = client.getRealSkillLevel(Skill.STRENGTH);
        int defence = client.getRealSkillLevel(Skill.DEFENCE);
        int hitpoints = client.getRealSkillLevel(Skill.HITPOINTS);
        int prayer = client.getRealSkillLevel(Skill.PRAYER);
        int ranged = client.getRealSkillLevel(Skill.RANGED);
        int magic = client.getRealSkillLevel(Skill.MAGIC);

        double base = 0.25 * (defence + hitpoints + Math.floor(prayer / 2.0));
        double melee = 0.325 * (attack + strength);
        double range = 0.325 * (Math.floor(ranged / 2.0) + ranged);
        double mage = 0.325 * (Math.floor(magic / 2.0) + magic);

        return (int) Math.floor(base + Math.max(melee, Math.max(range, mage)));
    }

    /**
     * Calculate total level
     */
    public int getTotalLevel() {
        int total = 0;
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) {
                total += client.getRealSkillLevel(skill);
            }
        }
        return total;
    }

    /**
     * Get bank data as a map
     */
    private Map<String, Object> getBankData() {
        ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);
        List<Map<String, Object>> items = new ArrayList<>();
        long totalValue = 0;

        if (bankContainer != null) {
            Item[] bankItems = bankContainer.getItems();
            for (int i = 0; i < bankItems.length; i++) {
                Item item = bankItems[i];
                if (item.getId() == -1 || item.getQuantity() == 0) {
                    continue;
                }

                ItemComposition comp = itemManager.getItemComposition(item.getId());
                int price = config.includeGePrices() ? itemManager.getItemPrice(item.getId()) : 0;

                Map<String, Object> itemData = new LinkedHashMap<>();
                itemData.put("id", item.getId());
                itemData.put("name", comp.getName());
                itemData.put("quantity", item.getQuantity());
                if (config.includeGePrices()) {
                    itemData.put("price", price);
                    totalValue += (long) price * item.getQuantity();
                }

                items.add(itemData);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("totalValue", totalValue);
        result.put("itemCount", items.size());
        return result;
    }

    /**
     * Get inventory data as a map
     */
    private Map<String, Object> getInventoryData() {
        ItemContainer invContainer = client.getItemContainer(InventoryID.INVENTORY);
        List<Map<String, Object>> items = new ArrayList<>();

        if (invContainer != null) {
            Item[] invItems = invContainer.getItems();
            for (int i = 0; i < 28 && i < invItems.length; i++) {
                if (invItems[i].getId() != -1) {
                    Item item = invItems[i];
                    ItemComposition comp = itemManager.getItemComposition(item.getId());
                    int price = config.includeGePrices() ? itemManager.getItemPrice(item.getId()) : 0;

                    Map<String, Object> itemData = new LinkedHashMap<>();
                    itemData.put("slot", i);
                    itemData.put("id", item.getId());
                    itemData.put("name", comp.getName());
                    itemData.put("quantity", item.getQuantity());
                    if (config.includeGePrices()) {
                        itemData.put("price", price);
                    }

                    items.add(itemData);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        return result;
    }

    /**
     * Get equipment data as a map
     */
    private Map<String, Object> getEquipmentData() {
        ItemContainer equipContainer = client.getItemContainer(InventoryID.EQUIPMENT);
        Map<String, Object> slots = new LinkedHashMap<>();

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

        if (equipContainer != null) {
            Item[] items = equipContainer.getItems();
            for (int i = 0; i < slotNames.length && i < slotIds.length; i++) {
                int slotIdx = slotIds[i];
                if (slotIdx < items.length && items[slotIdx].getId() != -1) {
                    Item item = items[slotIdx];
                    ItemComposition comp = itemManager.getItemComposition(item.getId());
                    int price = config.includeGePrices() ? itemManager.getItemPrice(item.getId()) : 0;

                    Map<String, Object> itemData = new LinkedHashMap<>();
                    itemData.put("id", item.getId());
                    itemData.put("name", comp.getName());
                    itemData.put("quantity", item.getQuantity());
                    if (config.includeGePrices()) {
                        itemData.put("price", price);
                    }

                    slots.put(slotNames[i], itemData);
                } else {
                    slots.put(slotNames[i], null);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("slots", slots);
        return result;
    }

    /**
     * Get quest data as a map
     */
    private Map<String, Object> getQuestData() {
        Map<String, String> quests = new LinkedHashMap<>();
        int completed = 0, inProgress = 0, notStarted = 0;

        for (Quest quest : Quest.values()) {
            QuestState state = quest.getState(client);
            quests.put(quest.getName(), state.name());
            switch (state) {
                case FINISHED:
                    completed++;
                    break;
                case IN_PROGRESS:
                    inProgress++;
                    break;
                case NOT_STARTED:
                    notStarted++;
                    break;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("quests", quests);
        result.put("completed", completed);
        result.put("inProgress", inProgress);
        result.put("notStarted", notStarted);
        result.put("total", Quest.values().length);
        return result;
    }

    /**
     * Get skill data as a map
     */
    private Map<String, Object> getSkillData() {
        Map<String, Object> skills = new LinkedHashMap<>();

        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) continue;

            Map<String, Object> skillData = new LinkedHashMap<>();
            skillData.put("level", client.getRealSkillLevel(skill));
            skillData.put("xp", client.getSkillExperience(skill));
            skillData.put("boostedLevel", client.getBoostedSkillLevel(skill));

            skills.put(skill.getName(), skillData);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("skills", skills);
        result.put("totalLevel", getTotalLevel());
        result.put("combatLevel", getCombatLevel());
        return result;
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

        Map<String, Object> bankData = getBankData();
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("username", getUsername());
        export.put("timestamp", Instant.now().toString());
        export.put("items", bankData.get("items"));
        export.put("totalValue", bankData.get("totalValue"));
        export.put("itemCount", bankData.get("itemCount"));

        writeJson("bank.json", export);
        updateLastExportTime();
        log.info("Exported {} bank items", bankData.get("itemCount"));
    }

    /**
     * Export inventory contents to JSON
     */
    public void exportInventory() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            log.warn("Cannot export inventory - not logged in");
            return;
        }

        Map<String, Object> invData = getInventoryData();
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("username", getUsername());
        export.put("timestamp", Instant.now().toString());
        export.put("items", invData.get("items"));

        writeJson("inventory.json", export);
        updateLastExportTime();
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

        Map<String, Object> equipData = getEquipmentData();
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("username", getUsername());
        export.put("timestamp", Instant.now().toString());
        export.put("slots", equipData.get("slots"));

        writeJson("equipment.json", export);
        updateLastExportTime();
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

        Map<String, Object> questData = getQuestData();
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("username", getUsername());
        export.put("timestamp", Instant.now().toString());
        export.put("quests", questData.get("quests"));
        export.put("summary", Map.of(
            "completed", questData.get("completed"),
            "inProgress", questData.get("inProgress"),
            "notStarted", questData.get("notStarted"),
            "total", questData.get("total")
        ));

        writeJson("quests.json", export);
        updateLastExportTime();
        log.info("Exported {} quests", questData.get("total"));
    }

    /**
     * Export skills to JSON
     */
    public void exportSkills() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            log.warn("Cannot export skills - not logged in");
            return;
        }

        Map<String, Object> skillData = getSkillData();
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("username", getUsername());
        export.put("timestamp", Instant.now().toString());
        export.put("skills", skillData.get("skills"));
        export.put("totalLevel", skillData.get("totalLevel"));
        export.put("combatLevel", skillData.get("combatLevel"));

        writeJson("skills.json", export);
        updateLastExportTime();
        log.info("Exported skills");
    }

    /**
     * Export combined Claude context file with all data and metadata
     */
    public void exportClaudeContext() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            log.warn("Cannot export Claude context - not logged in");
            return;
        }

        String timestamp = Instant.now().toString();
        String username = getUsername();

        // Meta information
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("description", "OSRS account data exported for Claude AI integration via MCP");
        meta.put("plugin", "Claude Export Plugin for RuneLite");
        meta.put("version", PLUGIN_VERSION);
        meta.put("exportTime", timestamp);
        meta.put("fields", Map.of(
            "skills", "Player skill levels, XP, and boosted levels",
            "quests", "Quest completion status (NOT_STARTED, IN_PROGRESS, FINISHED)",
            "bank", "Bank contents with item IDs, names, quantities, and GE prices",
            "inventory", "Current inventory items",
            "equipment", "Currently equipped gear"
        ));

        // Get all data
        Map<String, Object> skillData = getSkillData();
        Map<String, Object> questData = getQuestData();
        Map<String, Object> bankData = getBankData();
        Map<String, Object> invData = getInventoryData();
        Map<String, Object> equipData = getEquipmentData();

        // Summary for quick reference
        String summary = String.format("Username: %s | Total Level: %d | Combat: %d | Quests: %d/%d complete | Bank Value: %s GP",
            username,
            skillData.get("totalLevel"),
            skillData.get("combatLevel"),
            questData.get("completed"),
            questData.get("total"),
            formatNumber((Long) bankData.get("totalValue"))
        );

        // Build combined export
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("_meta", meta);
        export.put("_summary", summary);
        export.put("username", username);
        export.put("timestamp", timestamp);
        export.put("totalLevel", skillData.get("totalLevel"));
        export.put("combatLevel", skillData.get("combatLevel"));
        export.put("skills", skillData.get("skills"));
        export.put("quests", questData);
        export.put("bank", bankData);
        export.put("inventory", invData);
        export.put("equipment", equipData);

        writeJson("claude-context.json", export);
        updateLastExportTime();
        log.info("Exported Claude context file");
    }

    /**
     * Generate formatted summary text for copying to Claude
     */
    public String generateClaudeSummary() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return "Error: Not logged in to OSRS";
        }

        StringBuilder sb = new StringBuilder();
        String username = getUsername();

        // Header
        sb.append("=== OSRS Account Summary for Claude ===\n");
        sb.append(String.format("Username: %s\n", username));
        sb.append(String.format("Total Level: %d\n", getTotalLevel()));
        sb.append(String.format("Combat Level: %d\n\n", getCombatLevel()));

        // Skills in compact format
        sb.append("SKILLS:\n");
        String[] combatSkills = {"Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Prayer", "Magic"};
        StringBuilder combatLine = new StringBuilder();
        for (String skillName : combatSkills) {
            Skill skill = Skill.valueOf(skillName.toUpperCase());
            combatLine.append(String.format("%s %d | ", skillName, client.getRealSkillLevel(skill)));
        }
        sb.append(combatLine.toString().replaceAll(" \\| $", "\n"));

        String[] otherSkills = {"Cooking", "Woodcutting", "Fletching", "Fishing", "Firemaking", "Crafting", "Smithing",
            "Mining", "Herblore", "Agility", "Thieving", "Slayer", "Farming", "Runecraft", "Hunter", "Construction"};
        StringBuilder otherLine = new StringBuilder();
        int count = 0;
        for (String skillName : otherSkills) {
            Skill skill = Skill.valueOf(skillName.toUpperCase());
            otherLine.append(String.format("%s %d | ", skillName, client.getRealSkillLevel(skill)));
            count++;
            if (count % 4 == 0) {
                sb.append(otherLine.toString().replaceAll(" \\| $", "\n"));
                otherLine = new StringBuilder();
            }
        }
        if (otherLine.length() > 0) {
            sb.append(otherLine.toString().replaceAll(" \\| $", "\n"));
        }
        sb.append("\n");

        // Quest summary
        Map<String, Object> questData = getQuestData();
        @SuppressWarnings("unchecked")
        Map<String, String> quests = (Map<String, String>) questData.get("quests");

        List<String> completed = new ArrayList<>();
        List<String> inProgress = new ArrayList<>();

        for (Map.Entry<String, String> entry : quests.entrySet()) {
            if ("FINISHED".equals(entry.getValue())) {
                completed.add(entry.getKey());
            } else if ("IN_PROGRESS".equals(entry.getValue())) {
                inProgress.add(entry.getKey());
            }
        }

        sb.append(String.format("COMPLETED QUESTS (%d):\n", completed.size()));
        sb.append(String.join(", ", completed.subList(0, Math.min(10, completed.size()))));
        if (completed.size() > 10) {
            sb.append(String.format("... and %d more", completed.size() - 10));
        }
        sb.append("\n\n");

        if (!inProgress.isEmpty()) {
            sb.append(String.format("IN PROGRESS (%d):\n", inProgress.size()));
            sb.append(String.join(", ", inProgress));
            sb.append("\n\n");
        }

        // Bank summary
        Map<String, Object> bankData = getBankData();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bankItems = (List<Map<String, Object>>) bankData.get("items");
        sb.append("BANK SUMMARY:\n");
        sb.append(String.format("Total Value: ~%s GP\n", formatNumber((Long) bankData.get("totalValue"))));
        sb.append(String.format("Total Items: %d unique stacks\n", bankItems.size()));

        // Show top 5 most valuable items
        if (!bankItems.isEmpty() && config.includeGePrices()) {
            bankItems.sort((a, b) -> {
                long valA = ((Number) a.get("price")).longValue() * ((Number) a.get("quantity")).longValue();
                long valB = ((Number) b.get("price")).longValue() * ((Number) b.get("quantity")).longValue();
                return Long.compare(valB, valA);
            });
            sb.append("Notable Items: ");
            List<String> notable = new ArrayList<>();
            for (int i = 0; i < Math.min(5, bankItems.size()); i++) {
                Map<String, Object> item = bankItems.get(i);
                int qty = ((Number) item.get("quantity")).intValue();
                String name = (String) item.get("name");
                notable.add(qty > 1 ? name + " x" + qty : name);
            }
            sb.append(String.join(", ", notable));
            sb.append("\n");
        }
        sb.append("\n");

        // Current inventory
        Map<String, Object> invData = getInventoryData();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> invItems = (List<Map<String, Object>>) invData.get("items");
        sb.append("CURRENT INVENTORY:\n");
        if (invItems.isEmpty()) {
            sb.append("Empty\n");
        } else {
            List<String> invList = new ArrayList<>();
            for (Map<String, Object> item : invItems) {
                int qty = ((Number) item.get("quantity")).intValue();
                String name = (String) item.get("name");
                invList.add(qty > 1 ? name + " (" + formatNumber(qty) + ")" : name);
            }
            sb.append(String.join(", ", invList));
            sb.append("\n");
        }
        sb.append("\n");

        // Equipment
        Map<String, Object> equipData = getEquipmentData();
        @SuppressWarnings("unchecked")
        Map<String, Object> slots = (Map<String, Object>) equipData.get("slots");
        sb.append("EQUIPPED GEAR:\n");
        List<String> equipped = new ArrayList<>();
        for (Map.Entry<String, Object> entry : slots.entrySet()) {
            String slotName = entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1);
            if (entry.getValue() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) entry.getValue();
                equipped.add(slotName + ": " + item.get("name"));
            } else {
                equipped.add(slotName + ": None");
            }
        }
        sb.append(String.join(", ", equipped));
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Generate a starter prompt for Claude
     */
    public String generateClaudePrompt() {
        String summary = generateClaudeSummary();

        return "Here is my OSRS (Old School RuneScape) account data. I'm looking for advice to improve my account.\n\n" +
            summary + "\n\n" +
            "Based on my current stats, quest progress, and gear, can you help me with:\n" +
            "1. What quests should I prioritize next for good rewards?\n" +
            "2. Any gear upgrades I should work toward given my levels?\n" +
            "3. Efficient training methods for skills I should focus on?\n" +
            "4. Any account goals or milestones I should aim for?";
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
        if (config.exportClaudeContext()) {
            exportClaudeContext();
        }
        log.info("Exported all data for Claude MCP");
    }

    /**
     * Format large numbers with K/M suffixes
     */
    private String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    private String formatNumber(int number) {
        return formatNumber((long) number);
    }

    /**
     * Update the last export timestamp
     */
    private void updateLastExportTime() {
        lastExportTime = Instant.now();
        if (panel != null) {
            panel.updateLastExportLabel();
        }
    }

    /**
     * Get formatted last export time
     */
    public String getFormattedLastExportTime() {
        if (lastExportTime == null) {
            return "Never";
        }

        long secondsAgo = Instant.now().getEpochSecond() - lastExportTime.getEpochSecond();

        if (secondsAgo < 60) {
            return "Just now";
        } else if (secondsAgo < 3600) {
            long minutes = secondsAgo / 60;
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (secondsAgo < 86400) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a")
                .withZone(ZoneId.systemDefault());
            return formatter.format(lastExportTime);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
                .withZone(ZoneId.systemDefault());
            return formatter.format(lastExportTime);
        }
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

    /**
     * Get the MCP server URL from config
     */
    public String getMcpServerUrl() {
        return config.mcpServerUrl();
    }

    /**
     * Check if logged in
     */
    public boolean isLoggedIn() {
        return client.getGameState() == GameState.LOGGED_IN;
    }
}
