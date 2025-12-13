package xyz.superez.dynamickeepinv.rules;

import org.bukkit.World;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public class WorldTimeRule implements DeathRule {

    @Override
    public RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin) {
        World world = event.getEntity().getWorld();
        long time = world.getTime();
        long dayStart = plugin.getConfig().getLong("day-start", 0);
        long nightStart = plugin.getConfig().getLong("night-start", 13000);

        boolean isDay = plugin.isTimeInRange(time, dayStart, nightStart);
        String baseReason = isDay ? "time-day" : "time-night";
        String settingPath = isDay ? "advanced.day" : "advanced.night";

        boolean defaultKeepItems = getWorldKeepInventory(plugin, world, isDay);
        boolean keepItems = plugin.getConfig().getBoolean(settingPath + ".keep-items", defaultKeepItems);
        boolean keepXp = plugin.getConfig().getBoolean(settingPath + ".keep-xp", defaultKeepItems);

        return new RuleResult(keepItems, keepXp, baseReason);
    }

    private boolean getWorldKeepInventory(DynamicKeepInvPlugin plugin, World world, boolean isDay) {
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

    @Override
    public String getName() {
        return "WorldTimeRule";
    }
}
