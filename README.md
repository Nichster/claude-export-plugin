# Claude Export Plugin for RuneLite

Export your OSRS account data in formats optimized for Claude AI conversations and MCP integration.

## Why This Plugin?

Unlike generic data exporters, this plugin is **specifically designed for AI/LLM integration**:

- **Claude-Optimized Formats**: Exports include human-readable summaries and metadata that help Claude understand your account
- **One-Click Copy**: Copy your entire account summary formatted for pasting directly into Claude
- **Prompt Generation**: Generate starter prompts with your data ready for Claude conversations
- **MCP Server Integration**: Works with companion MCP servers for real-time Claude Desktop integration
- **Combined Context**: Single JSON file with all your data and metadata, ready for AI analysis

## Use Cases

Ask Claude for personalized OSRS advice:

- **Quest Planning**: "Based on my stats, what's the most efficient quest order?"
- **Gear Optimization**: "What upgrades should I prioritize for my budget?"
- **Account Reviews**: "Analyze my account and suggest goals"
- **Training Guides**: "What's the best way to train Slayer with my combat stats?"
- **Money Making**: "What money makers are available at my levels?"

## Features

### Claude AI Integration

| Feature | Description |
|---------|-------------|
| **Copy for Claude** | One-click copy of your formatted account summary to paste into any Claude conversation |
| **Generate Claude Prompt** | Creates a starter prompt with your data and suggested questions |
| **Export Claude Context** | Combined JSON file (`claude-context.json`) with metadata for MCP servers |

### Data Export

Export the following OSRS data to JSON files:

- **Skills** - All skill levels, XP, boosted levels, total level, and combat level
- **Quests** - Completion status for all quests with summary counts
- **Bank** - All items with quantities, GE prices, and total bank value
- **Inventory** - Current inventory contents with slot positions
- **Equipment** - All equipped items with prices

### Auto Export Options

- **On Login** - Automatically export skills and quests when logging in
- **On Change** - Export when bank/inventory/equipment changes
- **Interval** - Export all data every X minutes (configurable, 0-60 minutes)

## Installation

### From Plugin Hub
1. Open RuneLite
2. Go to the Plugin Hub (wrench icon)
3. Search for "Claude Export"
4. Click Install

### Manual Installation
1. Download the latest release JAR
2. Place in `~/.runelite/plugins/`
3. Restart RuneLite

## How to Use

### Quick Start: Copy for Claude

1. Log into OSRS
2. Click the Claude Export panel icon in the sidebar
3. Click **"Copy for Claude"**
4. Paste into any Claude conversation (claude.ai, Claude Desktop, API)
5. Ask Claude for advice!

### Example Output

When you click "Copy for Claude", you get a formatted summary like this:

```
=== OSRS Account Summary for Claude ===
Username: YourName
Total Level: 1547
Combat Level: 98

SKILLS:
Attack 75 | Strength 80 | Defence 70 | Hitpoints 77 | Ranged 82 | Prayer 55 | Magic 85
Cooking 70 | Woodcutting 65 | Fletching 70 | Fishing 60
...

COMPLETED QUESTS (87):
Dragon Slayer I, Monkey Madness I, Recipe for Disaster, ...

BANK SUMMARY:
Total Value: ~45.2M GP
Total Items: 342 unique stacks
Notable Items: Bandos chestplate, Abyssal whip, Dragon boots x2

CURRENT INVENTORY:
Coins (2,500,000), Shark x20, Super combat potion(4) x4

EQUIPPED GEAR:
Head: Slayer helmet (i), Body: Fighter torso, Legs: Obsidian platelegs, ...
```

### Generate Claude Prompt

Click **"Generate Claude Prompt"** to get a ready-to-use prompt:

```
Here is my OSRS (Old School RuneScape) account data. I'm looking for advice to improve my account.

[Your account data here]

Based on my current stats, quest progress, and gear, can you help me with:
1. What quests should I prioritize next for good rewards?
2. Any gear upgrades I should work toward given my levels?
3. Efficient training methods for skills I should focus on?
4. Any account goals or milestones I should aim for?
```

## Claude Desktop Integration (MCP)

For real-time integration with Claude Desktop, you can set up an MCP server that reads the exported data files. This allows Claude to access your live OSRS account data during conversations.

**[View Full MCP Setup Guide](MCP_SETUP.md)**

### How It Works

```
RuneLite + Plugin  ──►  JSON Files  ◄──  Claude Desktop + MCP Server
```

1. This plugin exports data to `~/.runelite/claude-export/`
2. An MCP server reads these JSON files
3. Claude Desktop connects to the MCP server
4. Claude can access your live OSRS data during conversations

### With MCP Integration, You Can Ask Claude:

- "What quests can I do with my current stats?"
- "Do I have the items needed for Dragon Slayer?"
- "What's my total bank value?"
- "Based on my levels, what should I train next?"

## Configuration

Access settings via RuneLite's configuration panel:

### Auto Export
- **Auto-export on login** - Export skills/quests after logging in
- **Auto-export on change** - Export when inventory/bank/equipment changes
- **Auto-export interval** - Export all data every X minutes (0 = disabled)

### Export Options
- **Include GE prices** - Include Grand Exchange prices in exports
- **Export Claude context file** - Also create combined `claude-context.json`

### MCP Integration
- **MCP Server Repository** - URL to your MCP server for reference

## Export Files

All files are saved to `~/.runelite/claude-export/`:

| File | Description |
|------|-------------|
| `skills.json` | Skill levels, XP, and combat stats |
| `quests.json` | Quest completion status with summary |
| `bank.json` | Bank contents with values |
| `inventory.json` | Current inventory |
| `equipment.json` | Equipped items |
| `claude-context.json` | **Combined file with all data + metadata** |

### Claude Context File Format

The `claude-context.json` file is specially formatted for AI consumption:

```json
{
  "_meta": {
    "description": "OSRS account data exported for Claude AI integration via MCP",
    "plugin": "Claude Export Plugin for RuneLite",
    "version": "1.1.0",
    "exportTime": "2026-01-02T15:30:00Z",
    "fields": {
      "skills": "Player skill levels, XP, and boosted levels",
      "quests": "Quest completion status (NOT_STARTED, IN_PROGRESS, FINISHED)",
      ...
    }
  },
  "_summary": "Username: Player | Total Level: 1547 | Combat: 98 | Quests: 87/158 complete | Bank Value: 45.2M GP",
  "username": "Player",
  "totalLevel": 1547,
  "combatLevel": 98,
  "skills": { ... },
  "quests": { ... },
  "bank": { ... },
  "inventory": { ... },
  "equipment": { ... }
}
```

## File Locations

- **Windows**: `%USERPROFILE%\.runelite\claude-export\`
- **macOS**: `~/.runelite/claude-export/`
- **Linux**: `~/.runelite/claude-export/`

## Requirements

- RuneLite client
- Old School RuneScape account
- (Optional) Claude Desktop with MCP for real-time integration

## Privacy & Security

- All data is stored locally on your computer
- No data is sent to external servers by this plugin
- You control when and what to share with Claude
- Bank values use public GE prices only

## Support

For issues, feature requests, or contributions:
- GitHub: https://github.com/Nichster/claude-export-plugin

## License

BSD 2-Clause License - See LICENSE file for details.

---

*This plugin is not affiliated with Jagex, Old School RuneScape, or Anthropic. RuneLite is a third-party client.*
