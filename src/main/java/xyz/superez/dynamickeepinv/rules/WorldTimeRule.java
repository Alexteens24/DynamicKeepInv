package xyz.superez.dynamickeepinv.rules;

import org.bukkit.World;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DKIConfig;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;
import xyz.superez.dynamickeepinv.ScheduleSupport;

public class WorldTimeRule implements DeathRule {

    @Override
    public RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin) {
        DKIConfig cfg = plugin.getDKIConfig();
        World world = event.getEntity().getWorld();
        long time = world.getTime();
        int seg = ScheduleSupport.segmentIndex(time, cfg.scheduleMilestones);
        ScheduleSupport.ScheduleMilestone m = cfg.scheduleMilestones.get(seg);

        boolean keepItems = m.keepItems();
        boolean keepXp = m.keepXp();

        DKIConfig.WorldTimeOverride override = cfg.worldOverrides.get(world.getName());
        if (override != null) {
            Boolean oi = override.resolveKeepItems(m.at(), seg);
            if (oi != null) {
                keepItems = oi;
            }
            Boolean ox = override.resolveKeepXp(m.at());
            if (ox != null) {
                keepXp = ox;
            }
        }

        return new RuleResult(keepItems, keepXp, RuleReasons.timeSegmentReason(seg));
    }

    @Override
    public String getName() {
        return "WorldTimeRule";
    }
}
