# Basic Configuration

This page covers the basic settings that most servers will use.

## Main Settings

```yaml
enabled: true              # Enable/disable the plugin
keep-inventory-day: true   # Keep inventory during day
keep-inventory-night: false # Keep inventory during night
check-interval: 100        # Check every 5 seconds (100 ticks)
```

## Time Settings

Minecraft day/night cycle uses ticks (0-23999):

| Time | Ticks | Description |
|------|-------|-------------|
| Sunrise | 0 | Day starts |
| Noon | 6000 | Midday |
| Sunset | 12000 | Evening |
| Night | 13000 | Night starts |
| Midnight | 18000 | Middle of night |

```yaml
day-start: 0       # When day begins (default: 0)
night-start: 13000 # When night begins (default: 13000)
```

## World Settings

### Enable for specific worlds only

```yaml
enabled-worlds:
  - world
  - world_nether
```

Leave empty `[]` to enable for all worlds.

### Per-world override

```yaml
world-settings:
  world_nether:
    keep-inventory-day: false
    keep-inventory-night: false
  world_the_end:
    keep-inventory-day: true
    keep-inventory-night: true
```

## Broadcast Settings

Notify players when day/night changes:

```yaml
broadcast:
  enabled: true
  day-change: true    # Announce when day starts
  night-change: true  # Announce when night starts
  chat: true          # Show in chat
  action-bar: false   # Show in action bar
  title: false        # Show as title
  sound:
    enabled: false
    day: "ENTITY_PLAYER_LEVELUP"
    night: "ENTITY_WITHER_SPAWN"
```

## Custom Trigger Times

If you want keep inventory to change at different times than day-start/night-start:

```yaml
gamerule-change:
  day-trigger: 1000   # Turn ON at tick 1000 (after sunrise)
  night-trigger: 12500 # Turn OFF at tick 12500 (before night)
```

Set to `-1` to use day-start/night-start values.
