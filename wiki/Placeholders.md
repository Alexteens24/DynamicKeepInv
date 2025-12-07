# Placeholders

DynamicKeepInv supports [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for use in scoreboards, holograms, chat formats, and more.

## Requirements

- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) installed on your server

No additional setup needed - placeholders are automatically registered when PlaceholderAPI is detected.

---

## Available Placeholders

### Status Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%dynamickeepinv_enabled%` | Plugin enabled | `true` / `false` |
| `%dynamickeepinv_keepinventory%` | Current keepInventory gamerule | `true` / `false` |
| `%dynamickeepinv_keepinventory_formatted%` | Formatted status | `ON` / `OFF` |

### Time Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%dynamickeepinv_time%` | Current world time (ticks) | `6000` |
| `%dynamickeepinv_isday%` | Is currently daytime | `true` / `false` |
| `%dynamickeepinv_isnight%` | Is currently nighttime | `true` / `false` |
| `%dynamickeepinv_period%` | Current time period | `Day` / `Night` |
| `%dynamickeepinv_period_vi%` | Period in Vietnamese | `Ngày` / `Đêm` |
| `%dynamickeepinv_period_en%` | Period in English | `Day` / `Night` |

### World Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%dynamickeepinv_world%` | Current world name | `world` |
| `%dynamickeepinv_world_enabled%` | World enabled for plugin | `true` / `false` |

### Player Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%dynamickeepinv_has_bypass%` | Player has bypass permission | `true` / `false` |

### Feature Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%dynamickeepinv_advanced_enabled%` | Advanced mode enabled | `true` / `false` |
| `%dynamickeepinv_economy_enabled%` | Economy feature enabled | `true` / `false` |
| `%dynamickeepinv_economy_cost%` | Economy cost | `100.0` |
| `%dynamickeepinv_lands_enabled%` | Lands integration enabled | `true` / `false` |
| `%dynamickeepinv_gp_enabled%` | GriefPrevention enabled | `true` / `false` |

### Stats Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%dynamickeepinv_stats_enabled%` | Stats system enabled | `true` / `false` |
| `%dynamickeepinv_stats_deaths_saved%` | Player's deaths saved | `15` |
| `%dynamickeepinv_stats_deaths_lost%` | Player's deaths lost | `3` |
| `%dynamickeepinv_stats_total_deaths%` | Player's total deaths | `18` |
| `%dynamickeepinv_stats_save_rate%` | Player's save rate | `83.3%` |
| `%dynamickeepinv_stats_economy_paid%` | Total money paid | `1500.00` |
| `%dynamickeepinv_stats_global_saved%` | Server total saved | `1234` |
| `%dynamickeepinv_stats_global_lost%` | Server total lost | `567` |
| `%dynamickeepinv_stats_global_rate%` | Server save rate | `68.5%` |

---

## Usage Examples

### Scoreboard (using TAB plugin)

```yaml
scoreboard:
  lines:
    - "&7Keep Inventory: &a%dynamickeepinv_keepinventory_formatted%"
    - "&7Time: &e%dynamickeepinv_period%"
```

### Hologram (using DecentHolograms)

```
/dh create keepinv
/dh line add keepinv "&b&lKeep Inventory"
/dh line add keepinv "&7Status: %dynamickeepinv_keepinventory_formatted%"
/dh line add keepinv "&7Period: %dynamickeepinv_period%"
```

### Chat Format (using EssentialsX)

```yaml
format: '{DISPLAYNAME} &7[%dynamickeepinv_period%]&r: {MESSAGE}'
```

### Conditional (using ConditionalEvents)

```yaml
check_keepinv:
  type: player_command
  conditions:
    - "%dynamickeepinv_isnight% == true"
  actions:
    - "message: &cWarning: You will lose items if you die at night!"
```

---

## Troubleshooting

### Placeholders not working

1. Make sure PlaceholderAPI is installed
2. Run `/papi reload`
3. Check `/papi list` - should show `dynamickeepinv`
4. Restart server if needed

### Shows %dynamickeepinv_...% literally

The plugin using placeholders might not support PlaceholderAPI. Check its documentation.

---

## Related

- [Installation](Installation) - Plugin setup
- [Basic Configuration](Basic-Configuration) - Time settings
