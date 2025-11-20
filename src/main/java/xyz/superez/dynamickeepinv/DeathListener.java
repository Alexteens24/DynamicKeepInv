package xyz.superez.dynamickeepinv;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {
    private final DynamicKeepInvPlugin plugin;

    public DeathListener(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("advanced.enabled", false)) {
            return;
        }

        Player player = event.getEntity();
        World world = player.getWorld();

        if (!plugin.isWorldEnabled(world)) {
            return;
        }

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

        if ((keepItems || keepXp) && plugin.getConfig().getBoolean("advanced.economy.enabled", false)) {
            double cost = plugin.getConfig().getDouble("advanced.economy.cost", 0.0);
            plugin.debug("Economy enabled. Cost=" + cost);
            if (cost > 0) {
                EconomyManager eco = plugin.getEconomyManager();
                if (eco == null) {
                    plugin.debug("EconomyManager is null, skipping economy check.");
                } else if (eco.isEnabled()) {
                    if (!eco.hasEnough(player, cost)) {
                        plugin.debug("Player " + player.getName() + " does not have enough money.");
                        String msg = plugin.getMessage("economy.not-enough-money")
                                .replace("{amount}", eco.format(cost));
                        player.sendMessage(plugin.parseMessage(msg));
                        keepItems = false;
                        keepXp = false;
                    } else {
                        plugin.debug("Charging player " + player.getName() + " " + cost);
                        boolean success = eco.withdraw(player, cost);
                        if (!success) {
                            plugin.debug("Economy withdrawal failed for player " + player.getName() + ".");
                            String msg = plugin.getMessage("economy.payment-failed")
                                .replace("{amount}", eco.format(cost));
                            player.sendMessage(plugin.parseMessage(msg));
                            keepItems = false;
                            keepXp = false;
                        } else {
                            String msg = plugin.getMessage("economy.paid")
                                .replace("{amount}", eco.format(cost));
                            player.sendMessage(plugin.parseMessage(msg));
                        }
                    }
                } else {
                    plugin.debug("Economy provider not found or disabled.");
                }
            }
        } else {
            plugin.debug("Economy check skipped. KeepItems=" + keepItems + ", KeepXP=" + keepXp + ", EcoEnabled=" + plugin.getConfig().getBoolean("advanced.economy.enabled", false));
        }

        plugin.debug("Final decision: keepItems=" + keepItems + ", keepXp=" + keepXp);
        
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
}
