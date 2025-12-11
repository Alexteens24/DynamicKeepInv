# Changelog

## [1.0.19] - 2025-12-10
### New Features
- **Auto-Pay Option** - Players can enable auto-pay to automatically keep items on death
  - Toggle with `/dki autopay` command or via GUI button
  - When enabled, automatically pays and keeps items without showing GUI
  - Falls back to GUI if player doesn't have enough money
  - Setting saved per-player in database

### Technical Changes
- Added `player_settings` table for storing auto-pay preferences
- Added auto-pay toggle button in Death Confirmation GUI (slot 8)

## [1.0.18] - 2025-12-10
### New Features
- **Death Confirmation GUI** - New economy mode `gui` that shows a GUI when player dies
  - Players can choose to pay to keep items or drop them
  - Configurable timeout (default: 30 seconds)
  - Persistent storage survives server restarts
  - Handles disconnect/reconnect gracefully
  - Protected against inventory duplication exploits
  - Command `/dki confirm` to reopen the GUI
- **Pending Death Database** - Separate SQLite database for pending deaths persistence

### Config Changes
- New economy mode: `mode: "gui"` (alongside existing `charge-to-keep` and `charge-to-bypass`)
- New GUI settings under `advanced.economy.gui`:
  - `timeout: 30` - Seconds before auto-drop
  - `expire-time: 300` - Seconds to store pending death if player disconnects

### Bug Fixes
- **Fixed timestamp not restored from DB** - Pending deaths loaded from database now use original timestamp instead of current time
- **Fixed ConcurrentModificationException** - Cleanup task now collects expired IDs before iterating
- **Fixed duplicate event listeners on reload** - Old DeathConfirmGUI listeners are now unregistered before creating new instance

### Technical Changes
- New classes: `PendingDeath`, `PendingDeathManager`, `DeathConfirmGUI`
- Added `EconomyManager.getBalance()` method
- Full Folia compatibility for GUI mode

## [1.0.17] - 2025-12-10
### Bug Fixes
- **Fixed ResultSet resource leak** - 6 database queries were not properly closing ResultSet objects, causing potential memory leaks
- **Fixed async task race condition** - Stats async tasks now check shutdown flag inside the task to prevent SQLException after connection close
- **Fixed economy retry spam** - Limited economy setup retries to 5 attempts (30s interval) instead of infinite retries
- **Fixed PlaceholderAPI version** - Now uses dynamic version from plugin instead of hardcoded "1.0.15"

### Improvements
- Economy retry counter resets on `/dki reload`
- Added debug logging for economy retry attempts
- Better shutdown safety for async database operations

## [1.0.16] - 2025-12-09
### Bug Fixes
- Fixed stats GUI title detection so inventory clicks/drags are always cancelled
- Fixed per-world keep-inventory resolution to use actual day/night state (not trigger window)
- Lands override now cleanly defers to Lands when `override-lands=false`, avoiding double handling
- Serialized all SQLite access to prevent concurrent connection use and potential locks
- Removed unnecessary deprecation suppression in stats command lookup


## [1.0.15] - 2025-12-07
### Bug Fixes
- Fixed economy payments not being tracked in stats database
- Fixed potential SQLite connection crashes on server lag
- Fixed `/dki stats <player>` not working for offline players

### Improvements
- Added async database writes for better Paper/Folia performance
- Database operations no longer block main server thread
- Graceful shutdown with async task completion

## [1.0.14] - 2025-12-07
### New Features
- Added Player Stats GUI (`/dki stats [player]`)
- SQLite database for persistent death statistics
- Track deaths saved, deaths lost, economy paid, death reasons
- 45-slot inventory GUI with player head, progress bar, server stats
- Added 8 new PlaceholderAPI placeholders for stats

### PlaceholderAPI Stats Placeholders
- `%dynamickeepinv_stats_deaths_saved%` - Total deaths saved
- `%dynamickeepinv_stats_deaths_lost%` - Total deaths lost
- `%dynamickeepinv_stats_total_deaths%` - Total deaths
- `%dynamickeepinv_stats_save_rate%` - Save rate percentage
- `%dynamickeepinv_stats_economy_paid%` - Total economy paid
- `%dynamickeepinv_stats_global_saved%` - Server-wide deaths saved
- `%dynamickeepinv_stats_global_lost%` - Server-wide deaths lost
- `%dynamickeepinv_stats_global_rate%` - Server-wide save rate

### Config Changes
- Config version updated to 5
- Added `stats.enabled` option

### Permissions
- `dynamickeepinv.stats` - View own stats
- `dynamickeepinv.stats.others` - View other players' stats

## [1.0.13] - 2025-12-04
### New Features
- Added PlaceholderAPI support with 14 placeholders
- Placeholders: `%dynamickeepinv_enabled%`, `%dynamickeepinv_keepinventory%`, `%dynamickeepinv_time%`, `%dynamickeepinv_isday%`, `%dynamickeepinv_isnight%`, `%dynamickeepinv_period%`, and more

## [1.0.12] - 2025-12-03
### New Features
- Added `use-death-cause` option for wilderness settings
- When enabled, wilderness deaths use death-cause (PvP/PvE) instead of fixed values
- Allows Lands to control claimed areas while death-cause controls wilderness

### Bug Fixes
- Fixed death messages showing during daytime when keepInventory gamerule is already true
- Plugin now skips advanced processing when gamerule handles everything

### Config Changes
- Config version updated to 4
- Added `wilderness.use-death-cause` for Lands and GriefPrevention

## [1.0.11] - 2025-12-03
- Simplified config.yml (removed verbose comments)
- Added wiki documentation
- Config version updated to 3

## [1.0.10] - 2025-12-03
### Bug Fixes
- Fixed death-cause not overriding wilderness settings
- PvP/PvE now correctly overrides wilderness keep-items/keep-xp
- Claimed areas (in-own-land, in-other-land) still have highest priority

### Improvements
- Updated priority order documentation in config
- Clearer comments explaining how settings interact

## [1.0.9] - 2025-12-03
### New Features
- Added per-world settings (world-settings section)
- Added wilderness config for Lands/GriefPrevention
- Added death message notifications (chat/action-bar)
- Added custom gamerule change times (day-trigger/night-trigger)

### Improvements
- Advanced settings now disabled by default (safer for new users)
- Nerfed Lands integration to avoid conflicts with Lands' built-in keepInventory
- Added override-lands option for explicit control
- Improved config comments explaining priority order
- Fixed economy bypass death message reason

### Config Changes
- Config version updated to 2 (auto-migration supported)
- Added gamerule-change section
- Added death-message section
- Added wilderness settings under protection plugins

## [1.0.8] - 2025-12-02
- Fixed thread-safety issues with protection hooks on Folia
- Fixed item duplication bug when forcing inventory drops
- Improved reload command to reinitialize economy and protection hooks
- Fixed null gamerule handling that could crash the scheduler

## [1.0.7] - 2025-11-27
- Add Lands plugin integration for land-based keep inventory
- Add GriefPrevention plugin integration for claim-based settings
- Separate settings for own/other land/claim
- Safe API version mismatch handling (auto-disable on incompatible)
- Downgrade to Java 17 for MC 1.20.4+ support

## [1.0.6] - 2025-11-27
- Added economy mode option (charge-to-keep / charge-to-bypass)
- Fixed DeathListener event priority
- Improved debug logging

## [1.0.5] - 2025-11-27
- Bug fixes

## [1.0.4] - 2025-11-20
- Fixed bypass permission path to handle plugins that return null drops
- Advanced day/night item and XP defaults now respect the base keep-inventory settings
- Economy manager initialization is now thread-safe across Folia regions

## [1.0.3] - 2025-11-20
- Hardened economy retry logic, event drop handling, and config wording for cleaner advanced flow
- Fixed Folia-safe player notification scheduling to avoid unsafe region access

## [1.0.2] - 2025-11-19
- Added advanced configuration for inventory and XP control
- Added bypass permission for specific players
- Added Vault economy integration for paid keep inventory
- Added separate PvP and PvE death rules
- Added action bar and title notifications
- Added configurable sound effects
- Updated messages.yml with economy notifications
- Refactored broadcast to target specific worlds
- Added DeathListener for advanced logic with Folia support
