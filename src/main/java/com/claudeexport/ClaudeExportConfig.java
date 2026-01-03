package com.claudeexport;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("claudeexport")
public interface ClaudeExportConfig extends Config {
    
    @ConfigItem(
        keyName = "autoExportOnLogin",
        name = "Auto-export on login",
        description = "Automatically export all data when logging in",
        position = 1
    )
    default boolean autoExportOnLogin() {
        return false;
    }
    
    @ConfigItem(
        keyName = "autoExportOnChange",
        name = "Auto-export on change",
        description = "Automatically export when bank/inventory/equipment changes",
        position = 2
    )
    default boolean autoExportOnChange() {
        return false;
    }
    
    @ConfigItem(
        keyName = "includeGePrices",
        name = "Include GE prices",
        description = "Include Grand Exchange prices in item exports",
        position = 3
    )
    default boolean includeGePrices() {
        return true;
    }
}
