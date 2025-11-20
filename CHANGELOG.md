# Changelog

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
