package xyz.superez.dynamickeepinv;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StatsListener implements Listener {
    private final DynamicKeepInvPlugin plugin;

    public StatsListener(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        StatsManager stats = plugin.getStatsManager();
        if (stats != null) {
            stats.loadStats(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        StatsManager stats = plugin.getStatsManager();
        if (stats != null) {
            stats.unloadStats(event.getPlayer().getUniqueId());
        }
    }
}
