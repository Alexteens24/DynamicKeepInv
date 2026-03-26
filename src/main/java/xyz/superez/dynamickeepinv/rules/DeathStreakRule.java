package xyz.superez.dynamickeepinv.rules;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DKIConfig;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grants keep-inventory to players on a death streak (N deaths within a time window).
 * config path: rules.streak.*
 *
 * Death timestamps are tracked in memory; they are lost on server restart.
 */
public class DeathStreakRule implements DeathRule {

    /** UUID → queue of death timestamps (ms) within the current window */
    private final Map<UUID, Deque<Long>> recentDeaths = new ConcurrentHashMap<>();

    @Override
    public RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin) {
        DKIConfig cfg = plugin.getDKIConfig();
        if (!cfg.deathStreakEnabled) return null;

        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long windowMs = (long) cfg.deathStreakWindowSec * 1000L;

        // Get or create the deque for this player
        Deque<Long> deque = recentDeaths.computeIfAbsent(uuid, k -> new ArrayDeque<>());

        // Evict timestamps outside the window
        while (!deque.isEmpty() && (now - deque.peekFirst()) > windowMs) {
            deque.pollFirst();
        }

        // Record this death
        deque.addLast(now);

        int count = deque.size();
        plugin.debug("DeathStreakRule: " + player.getName() + " has " + count + " deaths in window (" + cfg.deathStreakThreshold + " needed).");

        if (count >= cfg.deathStreakThreshold) {
            return new RuleResult(cfg.deathStreakKeepItems, cfg.deathStreakKeepXp, RuleReasons.DEATH_STREAK);
        }

        return null;
    }

    @Override
    public String getName() {
        return "DeathStreakRule";
    }

    /** Call on server shutdown or rule re-registration to free memory. */
    public void clear() {
        recentDeaths.clear();
    }
}
