# Frequently Asked Questions

## General

### Q: Does this work with Folia?
**A:** Yes! The plugin fully supports Folia's region-based scheduling.

### Q: What Minecraft versions are supported?
**A:** 1.20.4 and higher (requires Java 17+).

### Q: Will this conflict with other plugins?
**A:** Generally no. But if you use Lands or GriefPrevention, they have their own keep inventory features. See [Advanced Configuration](Advanced-Configuration) for how to handle this.

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
advanced:
  enabled: true
  protection:
    lands:
      enabled: true
      in-own-land:
        keep-items: true
        keep-xp: true
```

### Q: How do I make PvP not drop items but PvE does?
**A:**
```yaml
advanced:
  enabled: true
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
advanced:
  enabled: true
  night:
    keep-items: false
    keep-xp: true
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
3. `advanced.economy.enabled: true` in config

### Q: Settings seem to be ignored
**A:** Check the priority order:
1. Bypass permission (highest)
2. Claimed areas (in-own-land, in-other-land)
3. Death cause (PvP/PvE)
4. Wilderness
5. Time-based (lowest)

Higher priority settings override lower ones.

---

## Common Configs

### Hardcore Survival
```yaml
keep-inventory-day: false
keep-inventory-night: false
```

### Casual Survival
```yaml
keep-inventory-day: true
keep-inventory-night: true
```

### Day Safe, Night Dangerous
```yaml
keep-inventory-day: true
keep-inventory-night: false
```

### PvP Server (no item loss in PvP)
```yaml
advanced:
  enabled: true
  death-cause:
    enabled: true
    pvp:
      keep-items: true
      keep-xp: true
    pve:
      keep-items: false
      keep-xp: false
```
