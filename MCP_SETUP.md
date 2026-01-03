# MCP Server Setup Guide

Connect Claude Desktop to your OSRS account data in real-time using the Model Context Protocol (MCP).

## What is MCP?

MCP (Model Context Protocol) allows Claude Desktop to access external data sources. With an MCP server reading your exported OSRS data, Claude can:

- See your current skills, quests, bank, inventory, and equipment
- Provide personalized advice based on your actual account
- Help plan quests by checking your real requirements
- Suggest gear upgrades based on what you own

## How It Works

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   RuneLite +    │      │   JSON Files    │      │ Claude Desktop  │
│ Claude Export   │ ───► │ ~/.runelite/    │ ◄─── │   + MCP Server  │
│    Plugin       │      │ claude-export/  │      │                 │
└─────────────────┘      └─────────────────┘      └─────────────────┘
```

1. The Claude Export plugin exports your OSRS data to JSON files
2. An MCP server reads these JSON files
3. Claude Desktop connects to the MCP server
4. Claude can now access your live OSRS data

## Export File Locations

The plugin exports data to `~/.runelite/claude-export/`:

| File | Contents |
|------|----------|
| `skills.json` | All skill levels, XP, and boosted levels |
| `quests.json` | Quest completion status for all quests |
| `bank.json` | Bank contents with item names and quantities |
| `inventory.json` | Current inventory items |
| `equipment.json` | Currently equipped gear |
| `claude-context.json` | Combined file with all data + metadata |

## Setting Up the MCP Server

### Prerequisites

- Node.js 18+ installed
- Claude Desktop installed

### Step 1: Create the MCP Server

Create a new directory and initialize the project:

```bash
mkdir osrs-mcp-server
cd osrs-mcp-server
npm init -y
npm install @modelcontextprotocol/sdk
```

### Step 2: Create the Server Code

Create `index.js`:

```javascript
#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import fs from "fs/promises";
import path from "path";
import os from "os";

const EXPORT_DIR = path.join(os.homedir(), ".runelite", "claude-export");

const server = new McpServer({
  name: "osrs-mcp-server",
  version: "1.0.0",
});

// Helper to read JSON files
async function readExportFile(filename) {
  try {
    const filepath = path.join(EXPORT_DIR, filename);
    const data = await fs.readFile(filepath, "utf-8");
    return JSON.parse(data);
  } catch (error) {
    return null;
  }
}

// Tool: Get full account summary
server.tool("osrs_get_account", "Get full OSRS account summary including stats and quest progress", {}, async () => {
  const data = await readExportFile("claude-context.json");
  if (!data) return { content: [{ type: "text", text: "Account data not found. Make sure RuneLite is running with Claude Export plugin and click 'Export All'." }] };
  return { content: [{ type: "text", text: JSON.stringify(data, null, 2) }] };
});

// Tool: Get Skills
server.tool("osrs_get_skills", "Get current skill levels and XP", {}, async () => {
  const data = await readExportFile("skills.json");
  if (!data) return { content: [{ type: "text", text: "Skills data not found. Make sure RuneLite is running with Claude Export plugin." }] };
  return { content: [{ type: "text", text: JSON.stringify(data, null, 2) }] };
});

// Tool: Get Quests
server.tool("osrs_get_quests", "Get quest completion status", {
  filter: { type: "string", description: "Filter: 'all', 'finished', 'in_progress', 'not_started'", default: "all" }
}, async ({ filter = "all" }) => {
  const data = await readExportFile("quests.json");
  if (!data) return { content: [{ type: "text", text: "Quest data not found." }] };

  if (filter !== "all") {
    const statusMap = { finished: "FINISHED", in_progress: "IN_PROGRESS", not_started: "NOT_STARTED" };
    const filtered = Object.entries(data.quests)
      .filter(([_, status]) => status === statusMap[filter])
      .reduce((acc, [quest, status]) => ({ ...acc, [quest]: status }), {});
    return { content: [{ type: "text", text: JSON.stringify({ ...data, quests: filtered, filter }, null, 2) }] };
  }
  return { content: [{ type: "text", text: JSON.stringify(data, null, 2) }] };
});

// Tool: Get Bank
server.tool("osrs_get_bank", "Get bank contents", {
  search: { type: "string", description: "Search for items by name", default: "" }
}, async ({ search = "" }) => {
  const data = await readExportFile("bank.json");
  if (!data) return { content: [{ type: "text", text: "Bank data not found. Open your bank in-game and click 'Export Bank' or 'Export All'." }] };

  if (search) {
    const filtered = data.items.filter(item =>
      item.name.toLowerCase().includes(search.toLowerCase())
    );
    return { content: [{ type: "text", text: JSON.stringify({ ...data, items: filtered, search, matchCount: filtered.length }, null, 2) }] };
  }
  return { content: [{ type: "text", text: JSON.stringify(data, null, 2) }] };
});

// Tool: Get Inventory
server.tool("osrs_get_inventory", "Get current inventory", {}, async () => {
  const data = await readExportFile("inventory.json");
  if (!data) return { content: [{ type: "text", text: "Inventory data not found." }] };
  return { content: [{ type: "text", text: JSON.stringify(data, null, 2) }] };
});

// Tool: Get Equipment
server.tool("osrs_get_equipment", "Get equipped items", {}, async () => {
  const data = await readExportFile("equipment.json");
  if (!data) return { content: [{ type: "text", text: "Equipment data not found." }] };
  return { content: [{ type: "text", text: JSON.stringify(data, null, 2) }] };
});

// Start server
const transport = new StdioServerTransport();
await server.connect(transport);
```

### Step 3: Update package.json

Edit `package.json` to add the module type and bin entry:

```json
{
  "name": "osrs-mcp-server",
  "version": "1.0.0",
  "type": "module",
  "bin": {
    "osrs-mcp-server": "./index.js"
  },
  "dependencies": {
    "@modelcontextprotocol/sdk": "^1.0.0"
  }
}
```

### Step 4: Configure Claude Desktop

Edit your Claude Desktop config file:

**Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
**macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`

Add the MCP server configuration:

```json
{
  "mcpServers": {
    "osrs": {
      "command": "node",
      "args": ["C:/Users/YourName/osrs-mcp-server/index.js"]
    }
  }
}
```

Replace `C:/Users/YourName/osrs-mcp-server/index.js` with the actual path to your server.

### Step 5: Restart Claude Desktop

Close and reopen Claude Desktop. You should see the OSRS tools available in the tools menu.

## Testing the Integration

Once set up, try asking Claude:

- "What are my current OSRS skill levels?"
- "Which quests have I completed?"
- "Do I have a rune scimitar in my bank?"
- "What gear am I currently wearing?"
- "Based on my stats, what quests should I do next?"

Claude will use the MCP tools to fetch your actual game data and provide personalized responses.

## Available MCP Tools

| Tool | Description |
|------|-------------|
| `osrs_get_account` | Get full account summary (uses claude-context.json) |
| `osrs_get_skills` | Get all skill levels and XP |
| `osrs_get_quests` | Get quest status (can filter by completion) |
| `osrs_get_bank` | Get bank contents (can search by item name) |
| `osrs_get_inventory` | Get current inventory |
| `osrs_get_equipment` | Get equipped items |

## Keeping Data Fresh

The plugin exports data in several ways:

1. **Manual Export**: Click buttons in the plugin panel
2. **Auto-export on Login**: Enable in plugin settings
3. **Auto-export on Change**: Export when inventory/bank/equipment changes
4. **Auto-export Interval**: Export every X minutes (configurable)

For best results with MCP, enable auto-export so Claude always has recent data.

## Troubleshooting

### "Data not found" errors

- Make sure RuneLite is running with the Claude Export plugin enabled
- Click "Export All" in the plugin panel to ensure data is exported
- Check that files exist in `~/.runelite/claude-export/`

### MCP server not connecting

- Verify the path in `claude_desktop_config.json` is correct (use forward slashes)
- Check Claude Desktop logs for errors
- Make sure Node.js is installed and accessible from command line
- Try running the server manually: `node index.js`

### Data is outdated

- Enable auto-export in plugin settings for periodic updates
- Click "Export All" before asking Claude about your account
- Open your bank in-game before exporting bank data

### Bank shows empty

- You must have your bank interface open in-game when exporting
- Click "Export Bank" while the bank window is visible

## Need Help?

- [Plugin Issues](https://github.com/Nichster/claude-export-plugin/issues)
- [RuneLite Discord](https://discord.gg/runelite)
- [Claude MCP Documentation](https://modelcontextprotocol.io)
