package xyz.superez.dynamickeepinv.rules;

import org.bukkit.World;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DKIConfig;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public class WorldTimeRule implements DeathRule {

    @Override
    public RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin) {
        DKIConfig cfg = plugin.getDKIConfig();
        World world = event.getEntity().getWorld();
        long time = world.getTime();
        long dayStart = cfg.dayStart;
        long nightStart = cfg.nightStart;

        boolean isDay = plugin.isTimeInRange(time, dayStart, nightStart);
        String baseReason = isDay ? RuleReasons.TIME_DAY : RuleReasons.TIME_NIGHT;

        boolean keepItems = getWorldKeepInventory(plugin, cfg, world, isDay);
        boolean keepXp = isDay ? cfg.dayKeepXp : cfg.nightKeepXp;

        return new RuleResult(keepItems, keepXp, baseReason);
    }

    private boolean getWorldKeepInventory(DynamicKeepInvPlugin plugin, DKIConfig cfg, World world, boolean isDay) {
        String worldName = world.getName();
        String worldPath = "worlds.overrides." + worldName;

        if (plugin.getConfig().contains(worldPath)) {
            String timePath = isDay ? ".day" : ".night";
            if (plugin.getConfig().contains(worldPath + timePath)) {
                return plugin.getConfig().getBoolean(worldPath + timePath);
            }
        }

        // Fallback to global settings
        return isDay ? cfg.dayKeepItems : cfg.nightKeepItems;
    }

    @Override
    public String getName() {
        return "WorldTimeRule";
    }
}
