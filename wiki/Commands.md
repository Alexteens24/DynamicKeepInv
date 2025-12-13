# Commands

All commands for DynamicKeepInv.

## Main Command

| Command | Alias | Description | Permission |
|---------|-------|-------------|------------|
| `/dki` | `/dynamickeepinv`, `/keepinv` | Base command | `dynamickeepinv.use` |

## Subcommands

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/dki reload` | Reload configuration and messages | `dynamickeepinv.admin` |
| `/dki status` | View current status (enabled/disabled, time, world settings) | `dynamickeepinv.admin` |
| `/dki enable` | Enable the plugin globally | `dynamickeepinv.admin` |
| `/dki disable` | Disable the plugin globally | `dynamickeepinv.admin` |
| `/dki toggle` | Toggle the plugin on/off | `dynamickeepinv.admin` |

### User Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/dki stats` | View your own death statistics | `dynamickeepinv.stats` |
| `/dki stats <player>` | View another player's statistics | `dynamickeepinv.stats.others` |
| `/dki confirm` | Reopen the Death Confirmation GUI (if you have a pending death) | `dynamickeepinv.use` |
| `/dki autopay` | Toggle auto-pay mode for economy (pay automatically on death) | `dynamickeepinv.use` |

## Command Usage Examples

**Reloading the plugin:**
```
/dki reload
```

**Checking why items are being dropped:**
```
/dki status
```
*This shows if the plugin is enabled, if it's currently day or night, and the settings for the current world.*

**Viewing stats for a player:**
```
/dki stats Notch
```
