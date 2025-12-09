package xyz.superez.dynamickeepinv;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListener implements Listener {
    private final DynamicKeepInvPlugin plugin;

    public WorldListener(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        plugin.handleWorldUnload(event.getWorld());
    }
}
