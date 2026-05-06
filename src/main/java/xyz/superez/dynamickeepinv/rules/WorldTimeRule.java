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

        boolean keepItems = getWorldKeepInventory(cfg, world, isDay);
        boolean keepXp = isDay ? cfg.dayKeepXp : cfg.nightKeepXp;

        return new RuleResult(keepItems, keepXp, baseReason);
    }

    private boolean getWorldKeepInventory(DKIConfig cfg, World world, boolean isDay) {
        DKIConfig.WorldTimeOverride override = cfg.worldOverrides.get(world.getName());
        if (override != null) {
            Boolean value = isDay ? override.day() : override.night();
            if (value != null) return value;
        }
        return isDay ? cfg.dayKeepItems : cfg.nightKeepItems;
    }

    @Override
    public String getName() {
        return "WorldTimeRule";
    }
}
