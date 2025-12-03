# Changelog
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
