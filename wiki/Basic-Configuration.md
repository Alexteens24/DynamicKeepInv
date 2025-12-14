# Basic Configuration

This page covers the essential settings. For advanced features like death-cause rules and protection plugins, see [Advanced Configuration](Advanced-Configuration).

## Config File Location

After first run, config is at: `plugins/DynamicKeepInv/config.yml`

---

## Main Settings

```yaml
enabled: true              # Master switch for the plugin
keep-inventory-day: true   # Keep inventory during daytime
keep-inventory-night: false # Keep inventory during nighttime
check-interval: 100        # How often to check time (in ticks, 100 = 5 seconds)
```

| Setting | Description |
|---------|-------------|
| `enabled` | Turn the entire plugin on/off |
| `keep-inventory-day` | `true` = players keep items when dying during day |
| `keep-inventory-night` | `true` = players keep items when dying during night |
| `check-interval` | Lower = more accurate, but slightly more CPU usage |

---

## Time Settings

Minecraft uses ticks for time. One full day = 24000 ticks.

### Time Reference Chart

| Time | Ticks | What happens |
|------|-------|--------------|
| 🌅 Sunrise | 0 | Day starts, sun rises |
| ☀️ Noon | 6000 | Sun at highest point |
| 🌆 Sunset | 12000 | Sun begins to set |
| 🌙 Night | 13000 | Stars visible, mobs spawn |
| 🌑 Midnight | 18000 | Darkest point |
| 🌄 Dawn | 23000 | Sky starts to lighten |

### Configuration

```yaml
day-start: 0       # Tick when "day" begins
night-start: 13000 # Tick when "night" begins
```

**Example:** To make night start later (at sunset instead of dusk):
```yaml
night-start: 12000
```

---

## World Settings

### Option 1: Enable for specific worlds only

```yaml
enabled-worlds:
  - world
  - world_nether
```

Worlds not in this list will be ignored by the plugin.

> **Performance Tip (Folia):** On Folia servers, the plugin uses a periodic task to check time. Excluding a world from this list prevents the plugin from keeping that world "awake" or ticking unnecessarily, improving performance.

**Leave empty to enable for ALL worlds:**
```yaml
enabled-worlds: []
```

### Option 2: Per-world settings

Override day/night settings for specific worlds:

```yaml
world-settings:
  world_nether:
    keep-inventory-day: false    # Always drop in nether
    keep-inventory-night: false
  world_the_end:
    keep-inventory-day: true     # Always keep in end
    keep-inventory-night: true
```

This is useful when you want different rules for different dimensions.

---

## Broadcast Settings

Notify players when keepInventory changes:

```yaml
broadcast:
  enabled: true          # Enable notifications
  day-change: true       # Notify when day starts
  night-change: true     # Notify when night starts

  # Where to show the message
  chat: true             # In chat
  action-bar: false      # Above hotbar
  title: false           # Center of screen

  # Sound effects
  sound:
    enabled: false
    day: "ENTITY_PLAYER_LEVELUP"
    night: "ENTITY_WITHER_SPAWN"
```

### Available Sounds

Use any [Bukkit Sound](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html) name:
- `ENTITY_PLAYER_LEVELUP` - Level up sound
- `BLOCK_NOTE_BLOCK_PLING` - Note block pling
- `ENTITY_EXPERIENCE_ORB_PICKUP` - XP pickup
- `ENTITY_WITHER_SPAWN` - Dramatic wither sound

---

## Custom Trigger Times

By default, keepInventory changes at `day-start` and `night-start`. You can set different trigger times:

```yaml
gamerule-change:
  day-trigger: 1000    # Turn ON keepInventory at tick 1000
  night-trigger: 12500 # Turn OFF keepInventory at tick 12500
```

Set to `-1` to use the default `day-start` / `night-start` values.

**Use case:** You want "day" to be 0-13000 for mob spawning purposes, but keepInventory to change at different times.

---

## Messages

Edit `plugins/DynamicKeepInv/messages.yml` to customize messages:

```yaml
language: en  # 'en' for English, 'vi' for Vietnamese
```

All messages support color codes (`&a`, `&b`, etc.) and MiniMessage format.

---

## Next Steps

- [Advanced Configuration](Advanced-Configuration) - Death cause, protection plugins, economy
- [Commands](Commands) - Available commands
- [FAQ](FAQ) - Common questions
