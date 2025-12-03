package xyz.superez.dynamickeepinv;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.hooks.LandsHook;
import xyz.superez.dynamickeepinv.hooks.GriefPreventionHook;

public class DeathListener implements Listener {
    private final DynamicKeepInvPlugin plugin;

    public DeathListener(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("advanced.enabled", false)) {
            return;
        }

        Player player = event.getEntity();
        World world = player.getWorld();
        Location deathLocation = player.getLocation();

        if (!plugin.isWorldEnabled(world)) {
            return;
        }

        plugin.debug("Advanced death handling triggered for " + player.getName());
        plugin.debug("Current gamerule KEEP_INVENTORY: " + world.getGameRuleValue(org.bukkit.GameRule.KEEP_INVENTORY));
        plugin.debug("Event keepInventory before processing: " + event.getKeepInventory());

        if (plugin.getConfig().getBoolean("advanced.bypass-permission", true)) {
            if (player.hasPermission("dynamickeepinv.bypass")) {
                plugin.debug("Player " + player.getName() + " has bypass permission. Keeping inventory.");
                event.setKeepInventory(true);
                event.setKeepLevel(true);
                event.setDroppedExp(0);
                if (event.getDrops() != null) {
                    event.getDrops().clear();
                }
                sendDeathMessage(player, true, true, "bypass");
                return;
            }
        }

        ProtectionResult protectionResult = checkProtectionPlugins(player, deathLocation);
        if (protectionResult.handled) {
            plugin.debug("Death handled by protection plugin: keepItems=" + protectionResult.keepItems + ", keepXp=" + protectionResult.keepXp);
            applyKeepInventorySettings(event, protectionResult.keepItems, protectionResult.keepXp);
            sendDeathMessage(player, protectionResult.keepItems, protectionResult.keepXp, protectionResult.reason);
            return;
        }

        long time = world.getTime();
        long dayStart = plugin.getConfig().getLong("day-start", 0);
        long nightStart = plugin.getConfig().getLong("night-start", 13000);
        boolean isDay = plugin.isTimeInRange(time, dayStart, nightStart);
        boolean isNight = !isDay;
        plugin.debug("Death event: Time=" + time + ", dayStart=" + dayStart + ", nightStart=" + nightStart + ", isNight=" + isNight);

        String settingPath = isNight ? "advanced.night" : "advanced.day";
        boolean defaultKeepItems = getWorldKeepInventory(world, isDay);
        boolean keepItems = plugin.getConfig().getBoolean(settingPath + ".keep-items", defaultKeepItems);
        boolean keepXp = plugin.getConfig().getBoolean(settingPath + ".keep-xp", defaultKeepItems);

        if (plugin.getConfig().getBoolean("advanced.death-cause.enabled", false)) {
            boolean isPvp = player.getKiller() != null;
            String causePath = isPvp ? "advanced.death-cause.pvp" : "advanced.death-cause.pve";
            plugin.debug("Death cause enabled. isPvp=" + isPvp + " (cause: " + (isPvp ? "PvP" : "PvE") + ")");
            
            boolean oldKeepItems = keepItems;
            boolean oldKeepXp = keepXp;
            keepItems = plugin.getConfig().getBoolean(causePath + ".keep-items", keepItems);
            keepXp = plugin.getConfig().getBoolean(causePath + ".keep-xp", keepXp);
            
            if (oldKeepItems != keepItems || oldKeepXp != keepXp) {
                plugin.debug("Death cause OVERRIDE: keepItems " + oldKeepItems + " -> " + keepItems + ", keepXp " + oldKeepXp + " -> " + keepXp);
            }
        }

        final boolean baseKeepItems = keepItems;
        final boolean baseKeepXp = keepXp;

        if (plugin.getConfig().getBoolean("advanced.economy.enabled", false)) {
            double cost = plugin.getConfig().getDouble("advanced.economy.cost", 0.0);
            String mode = plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep");
            plugin.debug("Economy enabled. Cost=" + cost + ", Mode=" + mode);
            
            boolean shouldProcessEconomy = false;
            if ("charge-to-bypass".equalsIgnoreCase(mode)) {
                // Bypass mode: charge player to KEEP items when they would normally DROP
                shouldProcessEconomy = !keepItems || !keepXp;
                plugin.debug("Bypass mode check: keepItems=" + keepItems + ", keepXp=" + keepXp + ", shouldProcess=" + shouldProcessEconomy);
            } else {
                // Charge-to-keep mode: charge player when they WOULD keep items
                shouldProcessEconomy = keepItems || keepXp;
            }
            
            if (cost > 0 && shouldProcessEconomy) {
                EconomyManager eco = plugin.getEconomyManager();
                if (eco == null) {
                    plugin.debug("EconomyManager is null, skipping economy check.");
                } else if (eco.isEnabled()) {
                    if (!eco.hasEnough(player, cost)) {
                        plugin.debug("Player " + player.getName() + " does not have enough money.");
                        String msg = plugin.getMessage("economy.not-enough-money")
                                .replace("{amount}", eco.format(cost));
                        player.sendMessage(plugin.parseMessage(msg));
                        if ("charge-to-bypass".equalsIgnoreCase(mode)) {
                            plugin.debug("Bypass mode: Player cannot afford, using original keep settings.");
                            keepItems = baseKeepItems;
                            keepXp = baseKeepXp;
                        } else {
                            // charge-to-keep mode: can't pay = can't keep
                            keepItems = false;
                            keepXp = false;
                        }
                    } else {
                        plugin.debug("Charging player " + player.getName() + " " + cost);
                        boolean success = eco.withdraw(player, cost);
                        if (!success) {
                            plugin.debug("Economy withdrawal failed for player " + player.getName() + ".");
                            String msg = plugin.getMessage("economy.payment-failed")
                                .replace("{amount}", eco.format(cost));
                            player.sendMessage(plugin.parseMessage(msg));
                            if ("charge-to-bypass".equalsIgnoreCase(mode)) {
                                plugin.debug("Bypass mode: Payment failed, reverting to original keep settings.");
                                keepItems = baseKeepItems;
                                keepXp = baseKeepXp;
                            } else {
                                keepItems = false;
                                keepXp = false;
                            }
                        } else {
                            String msg = plugin.getMessage("economy.paid")
                                .replace("{amount}", eco.format(cost));
                            player.sendMessage(plugin.parseMessage(msg));
                            if ("charge-to-bypass".equalsIgnoreCase(mode)) {
                                plugin.debug("Bypass mode: Payment successful, keeping items.");
                                keepItems = true;
                                keepXp = true;
                            }
                            // charge-to-keep mode: payment success, keepItems/keepXp stay as configured
                        }
                    }
                } else {
                    plugin.debug("Economy provider not found or disabled.");
                }
            }
        } else {
            plugin.debug("Economy check skipped. EcoEnabled=" + plugin.getConfig().getBoolean("advanced.economy.enabled", false));
        }

        plugin.debug("Final decision: keepItems=" + keepItems + ", keepXp=" + keepXp);
        plugin.debug("Event keepInventory after processing: " + event.getKeepInventory());
        applyKeepInventorySettings(event, keepItems, keepXp);
        
        // Determine death message reason
        String reason;
        boolean economyBypass = plugin.getConfig().getBoolean("advanced.economy.enabled", false) 
                && "charge-to-bypass".equalsIgnoreCase(plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep"))
                && (keepItems || keepXp)
                && (!baseKeepItems || !baseKeepXp); // Player originally wouldn't keep, but now does
        
        if (economyBypass) {
            reason = "economy-bypass";
        } else if (plugin.getConfig().getBoolean("advanced.death-cause.enabled", false)) {
            reason = (player.getKiller() != null) ? "pvp" : "pve";
        } else {
            reason = isDay ? "time-day" : "time-night";
        }
        sendDeathMessage(player, keepItems, keepXp, reason);
        
        plugin.debug("Event keepInventory FINAL: " + event.getKeepInventory());
    }
    
    private boolean getWorldKeepInventory(World world, boolean isDay) {
        String worldName = world.getName();
        String worldPath = "world-settings." + worldName;
        
        if (plugin.getConfig().contains(worldPath)) {
            String timePath = isDay ? ".keep-inventory-day" : ".keep-inventory-night";
            if (plugin.getConfig().contains(worldPath + timePath)) {
                return plugin.getConfig().getBoolean(worldPath + timePath);
            }
        }
        
        // Fallback to global settings
        return isDay 
            ? plugin.getConfig().getBoolean("keep-inventory-day", true)
            : plugin.getConfig().getBoolean("keep-inventory-night", false);
    }
    
    private void sendDeathMessage(Player player, boolean keepItems, boolean keepXp, String reason) {
        if (!plugin.getConfig().getBoolean("advanced.death-message.enabled", true)) {
            return;
        }
        
        String messageKey;
        if (keepItems && keepXp) {
            messageKey = "death.keep-all";
        } else if (keepItems) {
            messageKey = "death.keep-items";
        } else if (keepXp) {
            messageKey = "death.keep-xp";
        } else {
            messageKey = "death.lost-all";
        }
        
        if ("bypass".equals(reason)) {
            messageKey = "death.bypass";
        }
        
        String message = plugin.getMessage(messageKey);
        String reasonMsg = plugin.getMessage("death." + reason);
        if (reasonMsg != null && !reasonMsg.startsWith("Missing message:")) {
            message = message + " " + reasonMsg;
        }
        
        if (plugin.getConfig().getBoolean("advanced.death-message.chat", true)) {
            player.sendMessage(plugin.parseMessage(message));
        }
        
        if (plugin.getConfig().getBoolean("advanced.death-message.action-bar", false)) {
            player.sendActionBar(plugin.parseMessage(message));
        }
    }
    
    private ProtectionResult checkProtectionPlugins(Player player, Location location) {
        plugin.debug("Checking protection plugins...");
        plugin.debug("Lands hook available: " + plugin.isLandsEnabled() + ", Config enabled: " + plugin.getConfig().getBoolean("advanced.protection.lands.enabled", false));
        
        if (plugin.isLandsEnabled() && plugin.getConfig().getBoolean("advanced.protection.lands.enabled", false)) {
            LandsHook lands = plugin.getLandsHook();
            boolean inLand = lands.isInLand(location);
            boolean overrideLands = plugin.getConfig().getBoolean("advanced.protection.lands.override-lands", false);
            plugin.debug("Player in Lands area: " + inLand + ", Override Lands settings: " + overrideLands);
            
            if (inLand) {
                // If override-lands is false, let Lands handle keep inventory in claimed areas
                if (!overrideLands) {
                    plugin.debug("In land but override-lands=false, letting Lands handle it.");
                    return new ProtectionResult(false, false, false, null);
                }
                
                plugin.debug("Player died in a Lands area: " + lands.getLandName(location));
                
                boolean isOwnLand = lands.isInOwnLand(player);
                String configPath = isOwnLand ? "advanced.protection.lands.in-own-land" : "advanced.protection.lands.in-other-land";
                
                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    plugin.debug("Lands settings for " + (isOwnLand ? "own" : "other") + " land: keepItems=" + keepItems + ", keepXp=" + keepXp);
                    String reason = isOwnLand ? "lands-own" : "lands-other";
                    return new ProtectionResult(true, keepItems, keepXp, reason);
                }
            } else {
                plugin.debug("Player died in WILDERNESS (outside any land)");
                // Check wilderness config for Lands
                if (plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.enabled", false)) {
                    boolean keepItems = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.keep-xp", false);
                    plugin.debug("Lands wilderness settings: keepItems=" + keepItems + ", keepXp=" + keepXp);
                    return new ProtectionResult(true, keepItems, keepXp, "lands-wilderness");
                }
            }
        }
        
        if (plugin.isGriefPreventionEnabled() && plugin.getConfig().getBoolean("advanced.protection.griefprevention.enabled", false)) {
            GriefPreventionHook gp = plugin.getGriefPreventionHook();
            if (gp.isInClaim(location)) {
                plugin.debug("Player died in a GriefPrevention claim owned by: " + gp.getClaimOwnerName(location));
                
                boolean isOwnClaim = gp.isInOwnClaim(player);
                String configPath = isOwnClaim ? "advanced.protection.griefprevention.in-own-claim" : "advanced.protection.griefprevention.in-other-claim";
                
                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    plugin.debug("GriefPrevention settings for " + (isOwnClaim ? "own" : "other") + " claim: keepItems=" + keepItems + ", keepXp=" + keepXp);
                    String reason = isOwnClaim ? "gp-own" : "gp-other";
                    return new ProtectionResult(true, keepItems, keepXp, reason);
                }
            } else {
                // Check wilderness config for GriefPrevention
                if (plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.enabled", false)) {
                    boolean keepItems = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.keep-xp", false);
                    plugin.debug("GriefPrevention wilderness settings: keepItems=" + keepItems + ", keepXp=" + keepXp);
                    return new ProtectionResult(true, keepItems, keepXp, "gp-wilderness");
                }
            }
        }
        
        return new ProtectionResult(false, false, false, null);
    }
    
    private void applyKeepInventorySettings(PlayerDeathEvent event, boolean keepItems, boolean keepXp) {
        Player player = event.getEntity();
        plugin.debug("applyKeepInventorySettings: keepItems=" + keepItems + ", keepXp=" + keepXp);
        plugin.debug("Current drops size: " + (event.getDrops() != null ? event.getDrops().size() : "null"));
        plugin.debug("Current keepInventory: " + event.getKeepInventory());
        
        if (keepItems) {
            event.setKeepInventory(true);
            if (event.getDrops() != null) {
                event.getDrops().clear();
            }
            plugin.debug("Set to KEEP inventory");
        } else {
            event.setKeepInventory(false);
            
            // Only force add inventory to drops if drops list is EMPTY
            // AND the gamerule keepInventory was true (server didn't create drops)
            // This prevents duplication when server already created drops
            Boolean gameruleKeepInv = player.getWorld().getGameRuleValue(org.bukkit.GameRule.KEEP_INVENTORY);
            boolean wasKeepingInventory = gameruleKeepInv != null && gameruleKeepInv;
            if (event.getDrops() != null && event.getDrops().isEmpty() && wasKeepingInventory) {
                plugin.debug("Drops empty and gamerule was keepInventory=true, forcing inventory to drops...");
                int addedItems = 0;
                for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        event.getDrops().add(item.clone());
                        addedItems++;
                    }
                }
                for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) {
                    if (item != null && !item.getType().isAir()) {
                        event.getDrops().add(item.clone());
                        addedItems++;
                    }
                }
                org.bukkit.inventory.ItemStack offhand = player.getInventory().getItemInOffHand();
                if (offhand != null && !offhand.getType().isAir()) {
                    event.getDrops().add(offhand.clone());
                    addedItems++;
                }
                plugin.debug("Added " + addedItems + " items to drops");
                player.getInventory().clear();
            } else {
                int dropSize = (event.getDrops() != null) ? event.getDrops().size() : 0;
                plugin.debug("Drops already exist (" + dropSize + " items), skipping force drop");
            }
            plugin.debug("Set to DROP inventory");
        }

        if (keepXp) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        } else {
            event.setKeepLevel(false);
            // Force drop XP if gamerule was keeping it (droppedExp would be 0)
            if (event.getDroppedExp() == 0) {
                int level = player.getLevel();
                int exp = Math.min(level * 7, 100);
                event.setDroppedExp(exp);
            }
        }
    }
    
    private static class ProtectionResult {
        final boolean handled;
        final boolean keepItems;
        final boolean keepXp;
        final String reason;
        
        ProtectionResult(boolean handled, boolean keepItems, boolean keepXp, String reason) {
            this.handled = handled;
            this.keepItems = keepItems;
            this.keepXp = keepXp;
            this.reason = reason;
        }
    }
}
