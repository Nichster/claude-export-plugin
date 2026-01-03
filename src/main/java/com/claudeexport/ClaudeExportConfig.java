package com.claudeexport;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("claudeexport")
public interface ClaudeExportConfig extends Config {

    @ConfigSection(
        name = "Auto Export",
        description = "Automatic export settings",
        position = 0
    )
    String autoExportSection = "autoExport";

    @ConfigItem(
        keyName = "autoExportOnLogin",
        name = "Auto-export on login",
        description = "Automatically export skills and quests when logging in",
        section = autoExportSection,
        position = 1
    )
    default boolean autoExportOnLogin() {
        return false;
    }

    @ConfigItem(
        keyName = "autoExportOnChange",
        name = "Auto-export on change",
        description = "Automatically export when bank/inventory/equipment changes",
        section = autoExportSection,
        position = 2
    )
    default boolean autoExportOnChange() {
        return false;
    }

    @ConfigItem(
        keyName = "autoExportInterval",
        name = "Auto-export interval (minutes)",
        description = "Automatically export all data every X minutes (0 = disabled)",
        section = autoExportSection,
        position = 3
    )
    @Range(min = 0, max = 60)
    default int autoExportInterval() {
        return 0;
    }

    @ConfigSection(
        name = "Export Options",
        description = "What to include in exports",
        position = 1
    )
    String exportOptionsSection = "exportOptions";

    @ConfigItem(
        keyName = "includeGePrices",
        name = "Include GE prices",
        description = "Include Grand Exchange prices in item exports",
        section = exportOptionsSection,
        position = 1
    )
    default boolean includeGePrices() {
        return true;
    }

    @ConfigItem(
        keyName = "exportClaudeContext",
        name = "Export Claude context file",
        description = "Also export a combined claude-context.json with all data",
        section = exportOptionsSection,
        position = 2
    )
    default boolean exportClaudeContext() {
        return true;
    }

    @ConfigSection(
        name = "MCP Integration",
        description = "Model Context Protocol settings",
        position = 2
    )
    String mcpSection = "mcp";

    @ConfigItem(
        keyName = "mcpServerUrl",
        name = "MCP Server Repository",
        description = "URL to your MCP server repository for Claude Desktop integration",
        section = mcpSection,
        position = 1
    )
    default String mcpServerUrl() {
        return "https://github.com/Nichster/osrs-mcp-server";
    }
}
