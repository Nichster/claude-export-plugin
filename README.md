# Claude Export Plugin

Export your Old School RuneScape game data to JSON files for use with Claude AI via Model Context Protocol (MCP).

## Features

This plugin exports the following OSRS data to JSON files:

- **Bank** - All items in your bank with quantities and Grand Exchange prices
- **Inventory** - Current inventory contents with slot positions
- **Equipment** - All equipped items
- **Quests** - Quest completion status for all quests
- **Skills** - Skill levels, XP, and boosted levels

All data is exported to `~/.runelite/claude-export/` directory for easy access by MCP servers.

## Use Case

This plugin is designed to work with Claude AI's Model Context Protocol (MCP). By exporting your OSRS data to JSON files, you can use an MCP server to let Claude read your game data and provide helpful assistance with:

- Inventory management and bank organization
- Quest planning and completion tracking
- Skill training recommendations
- Equipment optimization
- Item price lookups and wealth tracking

## How to Use

### Panel Interface

The plugin adds a sidebar panel with the following buttons:

- **Export All Data** - Exports bank, inventory, equipment, quests, and skills all at once
- **Export Bank** - Export only bank contents (requires bank to be open)
- **Export Inventory** - Export current inventory
- **Export Equipment** - Export equipped items
- **Export Quests** - Export quest completion status
- **Export Skills** - Export skill levels and XP
- **Open Export Folder** - Opens the export directory in your file manager

### Configuration

In the plugin configuration, you can enable:

- **Auto-export on login** - Automatically export all data when you log in
- **Auto-export on change** - Automatically export when bank/inventory/equipment changes
- **Include GE prices** - Include Grand Exchange prices in item exports (enabled by default)

## Export Format

All exports include:
- Username
- Timestamp (ISO 8601 format)
- Relevant game data

### Example Bank Export

```json
{
  "username": "PlayerName",
  "timestamp": "2024-01-02T18:30:00Z",
  "items": [
    {
      "id": 995,
      "name": "Coins",
      "quantity": 1000000,
      "price": 1,
      "tab": 0
    }
  ]
}
```

## Integration with MCP

To use this data with Claude AI:

1. Install an MCP server that reads from `~/.runelite/claude-export/`
2. Configure the MCP server in your Claude Desktop app or API integration
3. Claude can now read your OSRS game data to provide personalized assistance

## Export Location

All JSON files are saved to:
- **Windows**: `%USERPROFILE%\.runelite\claude-export\`
- **macOS/Linux**: `~/.runelite/claude-export/`

## Files Exported

- `bank.json` - Bank contents
- `inventory.json` - Inventory contents
- `equipment.json` - Equipped items
- `quests.json` - Quest statuses
- `skills.json` - Skill levels and XP

## Requirements

- RuneLite client
- Old School RuneScape account

## Support

For issues or feature requests, please visit the plugin's GitHub repository.

## License

This plugin is open source and follows RuneLite's plugin guidelines.
