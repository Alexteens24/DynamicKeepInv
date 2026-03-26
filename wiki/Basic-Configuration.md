# Basic Configuration

This page covers the essential settings. For advanced features like death-cause rules and protection plugins, see [Advanced Configuration](Advanced-Configuration).

## Config File Location

After first run, config is at: `plugins/DynamicKeepInv/config.yml`

---

## Main Settings

```yaml
enabled: true       # Master switch for the plugin
debug: false        # Log detailed rule decisions to console
check-interval: 100 # How often to check time (ticks; 100 = 5 seconds)
```

| Setting | Description |
|---------|-------------|
| `enabled` | Turn the entire plugin on/off without unloading it |
| `debug` | Prints which rule fired and why on every death — useful when diagnosing unexpected behaviour |
| `check-interval` | Lower = more accurate broadcast timing, but slightly more CPU usage |

---

## Keep Inventory Rules

The plugin decides whether to keep items and XP separately. Day/night settings are the baseline; other rules can override them (see [Advanced Configuration](Advanced-Configuration)).

```yaml
rules:
  day:
    keep-items: true  # Players keep items when dying during daytime
    keep-xp: true     # Players keep XP when dying during daytime
  night:
    keep-items: false # Players lose items when dying at night
    keep-xp: false    # Players lose XP when dying at night

  bypass-permission: true # dynamickeepinv.bypass always keeps everything
```

| Setting | Description |
|---------|-------------|
| `rules.day.keep-items` | `true` = items kept on day deaths |
| `rules.day.keep-xp` | `true` = XP kept on day deaths |
| `rules.night.keep-items` | `true` = items kept on night deaths |
| `rules.night.keep-xp` | `true` = XP kept on night deaths |
| `rules.bypass-permission` | When `true`, players with `dynamickeepinv.bypass` always keep everything |

---

## Time Settings

Minecraft uses ticks for time. One full day = 24,000 ticks.

### Time Reference Chart

| Time | Ticks | What happens |
|------|-------|--------------|
| Sunrise | 0 | Day starts, sun rises |
| Noon | 6000 | Sun at highest point |
| Sunset | 12000 | Sun begins to set |
| Night | 13000 | Stars visible, mobs spawn |
| Midnight | 18000 | Darkest point |
| Dawn | 23000 | Sky starts to lighten |

### Configuration

```yaml
time:
  day-start: 0       # Tick when "day" begins for rule purposes
  night-start: 13000 # Tick when "night" begins for rule purposes
  triggers:
    day: -1          # Tick to broadcast day change (-1 = use day-start)
    night: -1        # Tick to broadcast night change (-1 = use night-start)
```

**Example:** Make night start at sunset instead of dusk:
```yaml
time:
  night-start: 12000
```

**`triggers`** let you decouple when the *broadcast* fires from when the rule boundary is. Set to `-1` to use the corresponding `day-start`/`night-start` value.

---

## World Settings

### Enable for specific worlds only

```yaml
worlds:
  enabled:
    - world
    - world_nether
```

Worlds not listed are ignored by the plugin. Leave empty (`[]`) to enable for **all** worlds:

```yaml
worlds:
  enabled: []
```

### Per-world overrides

Override the day/night rule result for specific worlds:

```yaml
worlds:
  overrides:
    world_nether:
      day: false   # treat nether as "night" (drop items) regardless of time
      night: false
    world_the_end:
      day: true    # treat end as "day" (keep items) regardless of time
      night: true
```

`day`/`night` here map to `keep-items` for that world. XP follows the global rule.

---

## Broadcast Settings

Notify players when the keep-inventory period changes:

```yaml
messages:
  broadcast:
    enabled: true
    events:
      day-change: true    # Notify when day starts
      night-change: true  # Notify when night starts
    display:
      chat: true          # Broadcast in chat
      action-bar: false   # Broadcast above hotbar
      title: false        # Broadcast as screen title
    sound:
      enabled: false
      day: "ENTITY_PLAYER_LEVELUP"
      night: "ENTITY_WITHER_SPAWN"

  death:
    enabled: true   # Show a message to the dying player
    chat: true      # In chat
    action-bar: false
```

### Available Sounds

Use any [Bukkit Sound](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html) name:
- `ENTITY_PLAYER_LEVELUP` — level-up sound
- `BLOCK_NOTE_BLOCK_PLING` — note block pling
- `ENTITY_EXPERIENCE_ORB_PICKUP` — XP pickup
- `ENTITY_WITHER_SPAWN` — dramatic wither sound

---

## Messages

Edit `plugins/DynamicKeepInv/messages.yml` to customise messages:

```yaml
language: en  # 'en' for English, 'vi' for Vietnamese
```

All messages support MiniMessage format (e.g. `<green>`, `<bold>`) and legacy colour codes (`&a`, `&b`).

---

## Next Steps

- [Advanced Configuration](Advanced-Configuration) — Death cause rules, protection plugins, economy
- [Commands](Commands) — Available commands
- [FAQ](FAQ) — Common questions
