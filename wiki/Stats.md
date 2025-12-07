# Player Statistics

DynamicKeepInv tracks death statistics for every player. View stats through a beautiful GUI or use PlaceholderAPI.

## Requirements

Stats are enabled by default. No additional plugins required.

```yaml
stats:
  enabled: true
```

---

## Stats GUI

### Opening the GUI

| Command | Description | Permission |
|---------|-------------|------------|
| `/dki stats` | View your own stats | `dynamickeepinv.stats` |
| `/dki stats <player>` | View another player's stats | `dynamickeepinv.stats.others` |

### GUI Layout

```
┌─────────────────────────────────────────┐
│           DynamicKeepInv Stats          │
├─────────────────────────────────────────┤
│                                         │
│       [Player Head] PlayerName          │
│                                         │
│  💀 Total     💚 Saved    ❌ Lost       │
│    Deaths       Deaths      Deaths      │
│                                         │
│  📊 Save Rate      [Progress Bar]       │
│                                         │
│  💰 Economy Stats  ⏰ Last Death        │
│                                         │
│  📖 Death Breakdown                     │
│                                         │
│  ⭐ Server Stats           [Close]      │
│                                         │
└─────────────────────────────────────────┘
```

---

## Stats Tracked

### Per-Player Stats

| Stat | Description |
|------|-------------|
| Total Deaths | All deaths tracked by plugin |
| Deaths Saved | Deaths where inventory was kept |
| Deaths Lost | Deaths where inventory was dropped |
| Save Rate | Percentage of deaths saved |
| Economy Paid | Total money spent keeping inventory |
| Payment Count | Number of economy transactions |
| Last Death Time | When player last died |
| Last Death Reason | Why inventory was kept/lost |

### Per-Reason Breakdown

| Reason | Description |
|--------|-------------|
| Day | Deaths during daytime |
| Night | Deaths during nighttime |
| PvP | Deaths from player kills |
| PvE | Deaths from mobs/environment |
| Bypass | Deaths with bypass permission |
| Lands | Deaths in Lands areas |
| GriefPrevention | Deaths in GP claims |

### Global Stats

| Stat | Description |
|------|-------------|
| Global Saved | Total saved deaths server-wide |
| Global Lost | Total lost deaths server-wide |
| Global Rate | Server-wide save percentage |

---

## PlaceholderAPI

Use these placeholders in scoreboards, holograms, and chat:

| Placeholder | Output |
|-------------|--------|
| `%dynamickeepinv_stats_deaths_saved%` | `15` |
| `%dynamickeepinv_stats_deaths_lost%` | `3` |
| `%dynamickeepinv_stats_total_deaths%` | `18` |
| `%dynamickeepinv_stats_save_rate%` | `83.3%` |
| `%dynamickeepinv_stats_economy_paid%` | `1500.00` |
| `%dynamickeepinv_stats_global_saved%` | `1234` |
| `%dynamickeepinv_stats_global_lost%` | `567` |
| `%dynamickeepinv_stats_global_rate%` | `68.5%` |

See [Placeholders](Placeholders) for full list and examples.

---

## Database

Stats are stored in a SQLite database:

```
plugins/DynamicKeepInv/stats.db
```

### Database Structure

**player_stats table:**
- UUID, player name
- Deaths saved/lost/total
- Last death info
- Economy totals

**death_reasons table:**
- Per-reason saved/lost counts

### Backup

Simply copy `stats.db` to backup player statistics.

### Reset Player Stats

Currently no in-game command. To reset:
1. Stop server
2. Delete `stats.db` (resets all) or edit with SQLite tool
3. Start server

---

## Disable Stats

If you don't want stats tracking:

```yaml
stats:
  enabled: false
```

This will:
- Disable stats GUI (`/dki stats` will show error)
- Stop recording deaths
- Disable stats placeholders

---

## Related

- [Commands](Commands) - `/dki stats` command
- [Permissions](Permissions) - Stats permissions
- [Placeholders](Placeholders) - Stats placeholders
