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
| `/dki status` | View current status, world state, and active rule chain | `dynamickeepinv.admin` |
| `/dki enable` | Enable the plugin globally | `dynamickeepinv.admin` |
| `/dki disable` | Disable the plugin globally | `dynamickeepinv.admin` |
| `/dki toggle` | Toggle the plugin on/off | `dynamickeepinv.admin` |
| `/dki test [player]` | Diagnose which rule would currently apply to a player | `dynamickeepinv.admin` |

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
*This shows if the plugin is enabled, the active rule chain, and the current world states.*

**Diagnosing a specific player:**
```
/dki test Notch
```
*This prints the first rule that would currently match that player.*

**Viewing stats for a player:**
```
/dki stats Notch
```

## Notes

- `/dki confirm` and `/dki autopay` are only useful when `economy.mode: "gui"` is enabled.
- `/dki test` can be run from console if you provide a player name.
