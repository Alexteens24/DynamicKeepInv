# Changelog

## [1.0.3] - 2025-11-20

### Changed
- Code cleanup: Removed all inline comments from source files for cleaner codebase.

## [1.0.2] - 2025-11-19

### Added
- Advanced configuration section for granular control over inventory and XP preservation.
- Permission 'dynamickeepinv.bypass' to allow specific players to ignore inventory loss rules.
- Economy integration via Vault: Players can now pay a configurable cost to keep their inventory.
- Death Cause settings: Separate rules for PvP and PvE deaths.
- Visual notifications: Added support for Action Bar and Title messages.
- Sound effects: Configurable sounds for day/night transitions.
- 'DeathListener' to handle advanced logic while maintaining Folia compatibility.

### Changed
- Updated 'messages.yml' to include economy and new notification messages.
- Refactored broadcast logic to target specific worlds instead of global server broadcast.
- 'DynamicKeepInvPlugin' now exposes message parsing methods for external use.
