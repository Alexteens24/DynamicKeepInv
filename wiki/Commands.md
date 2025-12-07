# Commands

Base command: `/dynamickeepinv`  
Aliases: `/dki`, `/keepinv`

---

## Command Reference

| Command | Description | Permission |
|---------|-------------|------------|
| `/dki` | Show help menu | None |
| `/dki help` | Show help menu | None |
| `/dki status` | Show plugin status | `dynamickeepinv.admin` |
| `/dki reload` | Reload config files | `dynamickeepinv.admin` |
| `/dki enable` | Enable the plugin | `dynamickeepinv.admin` |
| `/dki disable` | Disable the plugin | `dynamickeepinv.admin` |
| `/dki toggle` | Toggle plugin on/off | `dynamickeepinv.admin` |
| `/dki stats` | View your death stats | `dynamickeepinv.stats` |
| `/dki stats <player>` | View player's stats | `dynamickeepinv.stats.others` |

---

## Command Details

### `/dki status`

Shows comprehensive plugin status:

```
=== DynamicKeepInv Status ===
Plugin: Enabled
World 'world': Day (tick 6000) - Keep Inventory: ON
World 'world_nether': Night (tick 18000) - Keep Inventory: OFF
World 'world_the_end': Keep Inventory: ON

Integrations:
  Lands: Enabled
  GriefPrevention: Disabled
  Economy: Disabled
```

Information displayed:
- Plugin enabled/disabled state
- Each world's current time (tick)
- Current keep inventory status per world
- Active plugin integrations

### `/dki reload`

Reloads configuration without server restart:
- `config.yml` - All settings
- `messages.yml` - All messages

**What gets reloaded:**
- Time settings
- World settings
- Broadcast settings
- Economy settings
- Protection plugin settings

**What requires full restart:**
- Initial Lands/GriefPrevention hook (if plugin wasn't detected on startup)

### `/dki enable` / `/dki disable` / `/dki toggle`

Control the plugin without editing config:

```
/dki disable  → Plugin paused, keepInventory won't change
/dki enable   → Plugin resumed
/dki toggle   → Flip current state
```

> **Note:** This is temporary. Plugin state resets to config value on restart.

---

### `/dki stats [player]`

Opens a GUI showing death statistics:

**Your own stats (`/dki stats`):**

```
┌─────────────────────────────────────────┐
│           DynamicKeepInv Stats          │
├─────────────────────────────────────────┤
│  [Head]  YourName                       │
│                                         │
│  💀 Total Deaths: 25                    │
│  💚 Deaths Saved: 18                    │
│  ❌ Deaths Lost: 7                      │
│  📊 Save Rate: 72%                      │
│  [████████░░] 72%                       │
│                                         │
│  💰 Economy: $1,500 paid (5 payments)   │
│  ⏰ Last Death: 06/12/2024 15:30        │
│     Reason: Day | Saved: Yes            │
│                                         │
│  📖 Breakdown:                          │
│     Day: 12 | Night: 8 | PvP: 5         │
│                                         │
│  ⭐ Server Stats:                       │
│     Saved: 1,234 | Lost: 567 | Rate: 68%│
└─────────────────────────────────────────┘
```

**View other player (`/dki stats Steve`):**
- Requires `dynamickeepinv.stats.others` permission
- Shows same GUI but for target player

---

## Console Commands

All commands work from console without the `/` prefix:

```
dki status
dki reload
```

---

## Related

- [Permissions](Permissions) - Permission nodes for commands
