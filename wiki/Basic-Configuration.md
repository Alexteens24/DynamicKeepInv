# Basic Configuration

This page covers the essential settings. For advanced features like death-cause rules, protection plugins, first-death, and economy, see [Advanced Configuration](Advanced-Configuration).

## Config File Location

After first run, your config is at `plugins/DynamicKeepInv/config.yml`.

---

## Main Settings

```yaml
enabled: true
debug: false
check-interval: 100
```

| Setting | Description |
|---------|-------------|
| `enabled` | Global master switch for the plugin |
| `debug` | Logs rule decisions to console |
| `check-interval` | How often to re-check time and update keepInventory gamerules |

---

## Keep Inventory Rules

Day/night settings are the baseline. Higher-priority rules can override them later.

```yaml
rules:
  day:
    keep-items: true
    keep-xp: true
  night:
    keep-items: false
    keep-xp: false

  bypass-permission: true
```

| Setting | Description |
|---------|-------------|
| `rules.day.keep-items` | Keep items during daytime |
| `rules.day.keep-xp` | Keep XP during daytime |
| `rules.night.keep-items` | Keep items during nighttime |
| `rules.night.keep-xp` | Keep XP during nighttime |
| `rules.bypass-permission` | Lets `dynamickeepinv.bypass` override all other gameplay rules |

---

## Time Settings

Minecraft uses ticks. One full day is 24,000 ticks.

| Time | Ticks | Meaning |
|------|-------|---------|
| Sunrise | 0 | Day starts |
| Noon | 6000 | Brightest point |
| Sunset | 12000 | Sun starts setting |
| Night | 13000 | Normal night start |
| Midnight | 18000 | Darkest point |
| Dawn | 23000 | Night ending |

```yaml
time:
  day-start: 0
  night-start: 13000
  triggers:
    day: -1
    night: -1
```

`triggers.day` and `triggers.night` control when the broadcast fires. Set them to `-1` to reuse `day-start` and `night-start`.

---

## World Settings

### Enable only specific worlds

```yaml
worlds:
  enabled:
    - world
    - world_nether
```

Leave it empty to enable DynamicKeepInv in all worlds:

```yaml
worlds:
  enabled: []
```

### Per-world overrides

Per-world overrides only affect the final day/night item decision:

```yaml
worlds:
  overrides:
    world_nether:
      day: false
      night: false
    world_the_end:
      day: true
      night: true
```

`day` and `night` here override `keep-items` only. XP still follows the configured day/night XP settings.

Higher-priority rules like bypass, first-death, streak, Lands, GP, WorldGuard, Towny, or death-cause rules can still override these values.

---

## Broadcast Settings

Use broadcasts to inform players when the keep-inventory state changes.

```yaml
messages:
  broadcast:
    enabled: true
    permission: ""
    events:
      day-change: true
      night-change: true
    display:
      chat: true
      action-bar: false
      title: false
    sound:
      enabled: false
      day: "ENTITY_PLAYER_LEVELUP"
      night: "ENTITY_WITHER_SPAWN"

  death:
    enabled: true
    chat: true
    action-bar: false
```

### Broadcast Permission Filter

If `messages.broadcast.permission` is empty, everyone sees the broadcast. If you set a permission node, only players with that permission receive the day/night broadcast.

Example:

```yaml
messages:
  broadcast:
    permission: "myserver.keepinv.alerts"
```

### Available Sounds

Use any valid Bukkit sound name, for example:

- `ENTITY_PLAYER_LEVELUP`
- `BLOCK_NOTE_BLOCK_PLING`
- `ENTITY_EXPERIENCE_ORB_PICKUP`
- `ENTITY_WITHER_SPAWN`

---

## Messages

Edit `plugins/DynamicKeepInv/messages.yml` to customise output.

```yaml
language: en
```

Messages support MiniMessage tags and legacy color codes.

---

## Next Steps

- [Advanced Configuration](Advanced-Configuration)
- [Commands](Commands)
- [FAQ](FAQ)
