# Frequently Asked Questions

## General

### Q: Does this work with Folia?
**A:** Yes! The plugin fully supports Folia's region-based scheduling.

### Q: What Minecraft versions are supported?
**A:** 1.20.4 and higher (requires Java 21+).

### Q: Will this conflict with other plugins?
**A:** Generally no. But if you use Lands, GriefPrevention, WorldGuard, or Towny, they may influence death handling in specific areas. See [Advanced Configuration](Advanced-Configuration).

---

## Configuration

### Q: My config changes aren't working
**A:** 
1. Run `/dki reload` 
2. Check console for errors
3. Make sure your YAML syntax is correct (use spaces, not tabs)

### Q: How do I make players always keep inventory in their own land?
**A:** 
```yaml
integrations:
  lands:
    enabled: true
    in-own-land:
      keep-items: true
      keep-xp: true
```

### Q: How do I make PvP not drop items but PvE does?
**A:**
```yaml
rules:
  death-cause:
    enabled: true
    pvp:
      keep-items: true
      keep-xp: true
    pve:
      keep-items: false
      keep-xp: true
```

### Q: I want night to drop items but still keep XP
**A:**
```yaml
rules:
  night:
    keep-items: false
    keep-xp: true
```

### Q: Can I protect a player's first death?
**A:** Yes.

```yaml
rules:
  first-death:
    enabled: true
    keep-items: true
    keep-xp: true
```

### Q: Can I reduce punishment if a player keeps dying repeatedly?
**A:** Yes, use the streak rule.

```yaml
rules:
  streak:
    enabled: true
    threshold: 3
    window-seconds: 300
    keep-items: false
    keep-xp: false
```

---

## Troubleshooting

### Q: Plugin says "Lands not found" but I have Lands installed
**A:** Make sure:
1. Lands is loading before DynamicKeepInv
2. You're using a compatible Lands version
3. Check console for API errors

### Q: Economy features not working
**A:** You need:
1. [Vault](https://www.spigotmc.org/resources/vault.34315/) plugin
2. An economy plugin (EssentialsX, CMI, etc.)
3. `economy.enabled: true` in config

### Q: Settings seem to be ignored
**A:** Check the priority order:
1. Bypass permission (highest)
2. First death
3. Death streak
4. Protection integrations
5. Death cause (PvP/PvE)
6. Time-based (lowest)

Higher priority settings override lower ones.

---

## Common Configs

### Hardcore Survival
```yaml
rules:
  day:
    keep-items: false
  night:
    keep-items: false
```

### Casual Survival
```yaml
rules:
  day:
    keep-items: true
  night:
    keep-items: true
```

### Day Safe, Night Dangerous
```yaml
rules:
  day:
    keep-items: true
  night:
    keep-items: false
```

### PvP Server (no item loss in PvP)
```yaml
rules:
  death-cause:
    enabled: true
    pvp:
      keep-items: true
      keep-xp: true
    pve:
      keep-items: false
      keep-xp: false
```

---

## Integration Questions

### Q: How do I use the Death Confirmation GUI?
**A:** Set economy mode to `gui`:
```yaml
economy:
  enabled: true
  cost: 100.0
  mode: "gui"
  gui:
    timeout: 30
    expire-time: 300
```

When players die:
1. GUI opens after respawn with Pay/Drop options
2. They have 30 seconds to decide
3. If they don't choose, items are dropped
4. If they disconnect, the pending death is saved for 5 minutes

### Q: Player closed the GUI accidentally. Can they reopen it?
**A:** Yes! They can use `/dki confirm` to reopen the confirmation GUI, as long as the timeout hasn't expired.

### Q: What happens if economy plugin is not available when using GUI mode?
**A:** Items will be dropped automatically. The GUI requires a working economy (Vault + economy plugin) to function.

---

### Q: I use Lands and want simple time-based rules. Do I need death-cause?
**A:** No! If you just want:
- Day = keep inventory
- Night = drop items
- Lands handles its own areas

Just disable death-cause:
```yaml
rules:
  death-cause:
    enabled: false

integrations:
  lands:
    enabled: true
    override-lands: false
```

This way:
- Inside Lands → Lands' keepInventory setting applies
- Outside Lands (wilderness) → Time-based rules apply
- No death-cause complexity

### Q: Death-cause vs Wilderness vs Lands - what takes priority?
**A:** Priority order (highest to lowest):
1. **Bypass permission** - `dynamickeepinv.bypass`
2. **First death**
3. **Death streak**
4. **Protection integrations** - Lands/GriefPrevention/WorldGuard/Towny
5. **Death cause** - PvP/PvE settings
6. **Time-based** - Day/Night rules

Example: Player dies at night, killed by another player, in wilderness:
- If `death-cause.enabled: true` → PvP settings apply
- If `death-cause.enabled: false` → Night settings apply

### Q: I want Lands to fully control its areas, plugin only controls wilderness
**A:** 
```yaml
integrations:
  lands:
    enabled: true
    override-lands: false
    wilderness:
      enabled: true
      keep-items: false
      keep-xp: true
```

With `override-lands: false`, the plugin won't touch areas inside Lands. Only wilderness (unclaimed) will use your settings.
