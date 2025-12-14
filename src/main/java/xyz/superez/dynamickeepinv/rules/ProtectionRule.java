package xyz.superez.dynamickeepinv.rules;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;
import xyz.superez.dynamickeepinv.hooks.GriefPreventionHook;
import xyz.superez.dynamickeepinv.hooks.LandsHook;

public class ProtectionRule implements DeathRule {

    @Override
    public RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin) {
        Player player = event.getEntity();
        Location location = player.getLocation();

        // 1. Check Lands
        if (plugin.isLandsEnabled() && plugin.getConfig().getBoolean("advanced.protection.lands.enabled", false)) {
            LandsHook lands = plugin.getLandsHook();
            boolean inLand = lands.isInLand(location);
            boolean overrideLands = plugin.getConfig().getBoolean("advanced.protection.lands.override-lands", false);

            if (inLand) {
                if (!overrideLands) {
                    // lands-defer: Return null to allow other plugins/rules or default behavior?
                    // In DeathListener, "defer" meant return immediately and do NOT process other logic.
                    // But here, returning null means "continue to next rule".
                    // If we want to defer to Lands, we should probably return a result that says "don't touch anything".
                    // Wait, if we return null, subsequent rules (like DeathCause, WorldTime) will run.
                    // But if Lands handles it, we shouldn't run those.
                    // "lands-defer" logic in DeathListener was: `return;` (stop processing).
                    // This means we should probably return a RuleResult that effectively says "Let external plugin handle it"
                    // BUT `RuleResult` only has keepItems/keepXp booleans.

                    // Actually, if we return null, we proceed to Death Cause / World Time.
                    // If Lands handles it, presumably Lands listens to the event itself?
                    // If so, we should probably NOT touch the event.

                    // In DeathListener:
                    // if ("lands-defer".equals(protectionResult.reason)) {
                    //     plugin.debug("Lands override disabled; deferring to Lands without altering drops.");
                    //     return;
                    // }

                    // So we need a way to say "STOP processing, but don't enforce keep/drop".
                    // However, RuleResult forces a boolean decision.
                    // Maybe we need a specific flag in RuleResult? Or maybe return null and ensure no other rules match?
                    // If we return null, DeathCause/Time rules will match.

                    // Let's look at how Lands works. Usually it listens to events.
                    // If we want to defer, we should probably abort our logic.
                    // But `RuleManager.evaluate` returns a `RuleResult` or `null`.
                    // If `null`, we assume no rule matched.

                    // If we want to support "defer", maybe we can return a special RuleResult?
                    // Or maybe we treat "defer" as "handled, but keep external state"?
                    // But we don't know the external state.

                    // Let's stick to the logic: if we defer, we should probably not set any flags.
                    // But we can't communicate "stop" without a result.

                    // Refactoring idea: Add a `handledExternal` flag to RuleResult?
                    // Or just return null and have a way to skip subsequent rules?
                    // But `RuleManager` iterates all.

                    // Wait, if `override-lands` is false, it means we respect Lands settings.
                    // If Lands settings are "drop", we shouldn't force "keep".
                    // If Lands settings are "keep", we shouldn't force "drop".

                    // If we return null, the next rules (DeathCause, WorldTime) will run and likely return a Result.
                    // That Result will be applied.
                    // Example: Time says "Keep". Lands says "Drop" (internally).
                    // If we return null, Time rule runs -> returns Keep -> we apply Keep.
                    // This OVERRIDES Lands.

                    // So "defer" MUST return a Result that prevents further processing.
                    // But what result?
                    // If we return Keep=false, Xp=false, we are forcing drop.
                    // If we return Keep=true, Xp=true, we are forcing keep.

                    // The original code `return` implies "Do nothing more in this listener".
                    // So we effectively let the event stay as it is (managed by other plugins or default gamerule).
                    // So we need a way to signal "Abort".

                    // Let's modify RuleResult to have an `abort` or `defer` flag.
                    // But I can't modify `RuleResult` easily without changing all usages.
                    // Actually I just created it.

                    // Let's check `DeathListener` again.
                    // It returns `void`. So it stops modifying the event.

                    // I will modify `RuleResult` to add `boolean defer`.
                    // Or `boolean isFinal`.

                    // Actually, let's just use a special reason "defer".
                    // And in `DeathListener` (or wherever consumes the result), check for "defer".

                    return new RuleResult(false, false, "lands-defer");
                    // NOTE: keepItems=false might mean "force drop".
                    // But if we handle "lands-defer" specially in the consumer, we can ignore the booleans.
                }

                boolean isOwnLand = lands.isInOwnLand(player);
                String configPath = isOwnLand ? "advanced.protection.lands.in-own-land" : "advanced.protection.lands.in-other-land";

                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    String reason = isOwnLand ? "lands-own" : "lands-other";
                    return new RuleResult(keepItems, keepXp, reason);
                }
            } else {
                // Wilderness
                if (plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.enabled", false)) {
                    boolean useDeathCause = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.use-death-cause", false);
                    if (!useDeathCause) {
                         boolean keepItems = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.keep-items", false);
                         boolean keepXp = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.keep-xp", false);
                         return new RuleResult(keepItems, keepXp, "lands-wilderness");
                    }
                    // If useDeathCause is true, we return null to let DeathCauseRule handle it.
                }
            }
        }

        // 2. Check GriefPrevention
        if (plugin.isGriefPreventionEnabled() && plugin.getConfig().getBoolean("advanced.protection.griefprevention.enabled", false)) {
            GriefPreventionHook gp = plugin.getGriefPreventionHook();
            if (gp.isInClaim(location)) {
                boolean isOwnClaim = gp.isInOwnClaim(player);
                String configPath = isOwnClaim ? "advanced.protection.griefprevention.in-own-claim" : "advanced.protection.griefprevention.in-other-claim";

                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    String reason = isOwnClaim ? "gp-own" : "gp-other";
                    return new RuleResult(keepItems, keepXp, reason);
                }
            } else {
                 if (plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.enabled", false)) {
                     boolean useDeathCause = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.use-death-cause", false);
                     if (!useDeathCause) {
                         boolean keepItems = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.keep-items", false);
                         boolean keepXp = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.keep-xp", false);
                         return new RuleResult(keepItems, keepXp, "gp-wilderness");
                     }
                 }
            }
        }

        return null;
    }

    @Override
    public String getName() {
        return "ProtectionRule";
    }
}
