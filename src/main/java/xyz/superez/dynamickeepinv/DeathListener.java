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

        if (plugin.getConfig().getBoolean("advanced.bypass-permission", true)) {
            if (player.hasPermission("dynamickeepinv.bypass")) {
                plugin.debug("Player " + player.getName() + " has bypass permission. Keeping inventory.");
                event.setKeepInventory(true);
                event.setKeepLevel(true);
                event.setDroppedExp(0);
                if (event.getDrops() != null) {
                    event.getDrops().clear();
                }
                return;
            }
        }

        ProtectionResult protectionResult = checkProtectionPlugins(player, deathLocation);
        if (protectionResult.handled) {
            plugin.debug("Death handled by protection plugin: keepItems=" + protectionResult.keepItems + ", keepXp=" + protectionResult.keepXp);
            applyKeepInventorySettings(event, protectionResult.keepItems, protectionResult.keepXp);
            return;
        }

        long time = world.getTime();
        long dayStart = plugin.getConfig().getLong("day-start", 0);
        long nightStart = plugin.getConfig().getLong("night-start", 13000);
        boolean isDay = plugin.isTimeInRange(time, dayStart, nightStart);
        boolean isNight = !isDay;
        plugin.debug("Death event: Time=" + time + ", dayStart=" + dayStart + ", nightStart=" + nightStart + ", isNight=" + isNight);

        String settingPath = isNight ? "advanced.night" : "advanced.day";
        boolean defaultKeepItems = isDay
            ? plugin.getConfig().getBoolean("keep-inventory-day", true)
            : plugin.getConfig().getBoolean("keep-inventory-night", false);
        boolean keepItems = plugin.getConfig().getBoolean(settingPath + ".keep-items", defaultKeepItems);
        boolean keepXp = plugin.getConfig().getBoolean(settingPath + ".keep-xp", defaultKeepItems);

        if (plugin.getConfig().getBoolean("advanced.death-cause.enabled", false)) {
            boolean isPvp = player.getKiller() != null;
            String causePath = isPvp ? "advanced.death-cause.pvp" : "advanced.death-cause.pve";
            plugin.debug("Death cause enabled. isPvp=" + isPvp);
            
            keepItems = plugin.getConfig().getBoolean(causePath + ".keep-items", keepItems);
            keepXp = plugin.getConfig().getBoolean(causePath + ".keep-xp", keepXp);
        }

        if (plugin.getConfig().getBoolean("advanced.economy.enabled", false)) {
            double cost = plugin.getConfig().getDouble("advanced.economy.cost", 0.0);
            String mode = plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep");
            plugin.debug("Economy enabled. Cost=" + cost + ", Mode=" + mode);
            
            boolean shouldProcessEconomy = false;
            if ("charge-to-bypass".equalsIgnoreCase(mode)) {
                shouldProcessEconomy = !keepItems || !keepXp;
                plugin.debug("Bypass mode check: keepItems=" + keepItems + ", keepXp=" + keepXp + ", shouldProcess=" + shouldProcessEconomy);
            } else {
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
                            plugin.debug("Bypass mode: Player cannot afford, items will drop.");
                        } else {
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
                                plugin.debug("Bypass mode: Payment failed, items will drop.");
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
        applyKeepInventorySettings(event, keepItems, keepXp);
    }
    
    private ProtectionResult checkProtectionPlugins(Player player, Location location) {
        if (plugin.isLandsEnabled() && plugin.getConfig().getBoolean("advanced.protection.lands.enabled", false)) {
            LandsHook lands = plugin.getLandsHook();
            if (lands.isInLand(location)) {
                plugin.debug("Player died in a Lands area: " + lands.getLandName(location));
                
                boolean isOwnLand = lands.isInOwnLand(player);
                String configPath = isOwnLand ? "advanced.protection.lands.in-own-land" : "advanced.protection.lands.in-other-land";
                
                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    plugin.debug("Lands settings for " + (isOwnLand ? "own" : "other") + " land: keepItems=" + keepItems + ", keepXp=" + keepXp);
                    return new ProtectionResult(true, keepItems, keepXp);
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
                    return new ProtectionResult(true, keepItems, keepXp);
                }
            }
        }
        
        return new ProtectionResult(false, false, false);
    }
    
    private void applyKeepInventorySettings(PlayerDeathEvent event, boolean keepItems, boolean keepXp) {
        if (keepItems) {
            event.setKeepInventory(true);
            if (event.getDrops() != null) {
                event.getDrops().clear();
            }
        } else {
            event.setKeepInventory(false);
        }

        if (keepXp) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        } else {
            event.setKeepLevel(false);
        }
    }
    
    private static class ProtectionResult {
        final boolean handled;
        final boolean keepItems;
        final boolean keepXp;
        
        ProtectionResult(boolean handled, boolean keepItems, boolean keepXp) {
            this.handled = handled;
            this.keepItems = keepItems;
            this.keepXp = keepXp;
        }
    }
}
