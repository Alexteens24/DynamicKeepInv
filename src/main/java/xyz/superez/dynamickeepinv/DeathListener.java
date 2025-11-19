package xyz.superez.dynamickeepinv;

import org.bukkit.GameRule;
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

        // Check if plugin is enabled in this world
        if (!plugin.isWorldEnabled(world)) {
            return;
        }

        // Check Bypass Permission
        if (plugin.getConfig().getBoolean("advanced.bypass-permission", true)) {
            if (player.hasPermission("dynamickeepinv.bypass")) {
                // Player has bypass, keep everything
                event.setKeepInventory(true);
                event.setKeepLevel(true);
                event.setDroppedExp(0);
                event.getDrops().clear();
                return;
            }
        }

        // Determine if it is Day or Night based on plugin logic
        long time = world.getTime();
        long nightStart = plugin.getConfig().getLong("night-start", 13000);
        
        boolean isNight = time >= nightStart && time < 24000;
        // Note: This simple check assumes standard day/night cycle. 
        // If day-start > night-start (wrapping), logic needs to be more complex, 
        // but for now we stick to the config structure.

        String settingPath = isNight ? "advanced.night" : "advanced.day";
        
        boolean keepItems = plugin.getConfig().getBoolean(settingPath + ".keep-items", !isNight);
        boolean keepXp = plugin.getConfig().getBoolean(settingPath + ".keep-xp", !isNight);

        // Death Cause Logic
        if (plugin.getConfig().getBoolean("advanced.death-cause.enabled", false)) {
            boolean isPvp = player.getKiller() != null;
            String causePath = isPvp ? "advanced.death-cause.pvp" : "advanced.death-cause.pve";
            
            // Override time-based settings
            keepItems = plugin.getConfig().getBoolean(causePath + ".keep-items", keepItems);
            keepXp = plugin.getConfig().getBoolean(causePath + ".keep-xp", keepXp);
        }

        // Economy Handling
        if ((keepItems || keepXp) && plugin.getConfig().getBoolean("advanced.economy.enabled", false)) {
            double cost = plugin.getConfig().getDouble("advanced.economy.cost", 0.0);
            if (cost > 0) {
                EconomyManager eco = plugin.getEconomyManager();
                // Only charge if economy is actually enabled/hooked
                if (eco.isEnabled()) {
                    if (!eco.hasEnough(player, cost)) {
                        // Not enough money -> Lose items/xp
                        String msg = plugin.getMessage("economy.not-enough-money")
                                .replace("{amount}", eco.format(cost));
                        player.sendMessage(plugin.parseMessage(msg));
                        keepItems = false;
                        keepXp = false;
                    } else {
                        // Withdraw and notify
                        eco.withdraw(player, cost);
                        String msg = plugin.getMessage("economy.paid")
                                .replace("{amount}", eco.format(cost));
                        player.sendMessage(plugin.parseMessage(msg));
                    }
                }
            }
        }

        // Apply Granular Settings
        
        // Handle Items
        if (keepItems) {
            event.setKeepInventory(true);
            event.getDrops().clear();
        } else {
            // If we want to force drop, we rely on GameRule being false.
            // If GameRule is true, we can't easily force drop without more complex logic.
            // So we only explicitly SAVE items here.
            if (Boolean.FALSE.equals(world.getGameRuleValue(GameRule.KEEP_INVENTORY))) {
                event.setKeepInventory(false);
            }
        }

        // Handle XP
        if (keepXp) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        } else {
            // If we want to force lose XP
             if (Boolean.FALSE.equals(world.getGameRuleValue(GameRule.KEEP_INVENTORY))) {
                event.setKeepLevel(false);
            }
        }
    }
}
