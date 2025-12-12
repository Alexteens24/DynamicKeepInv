package xyz.superez.dynamickeepinv.hooks;

import dev.cwhead.GravesX.GravesXAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

import java.util.Collections;
import java.util.List;

public class GravesXHook {
    private final DynamicKeepInvPlugin plugin;
    private GravesXAPI gravesXAPI;
    private boolean enabled;

    public GravesXHook(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("GravesX") == null) {
            return false;
        }

        try {
            org.bukkit.plugin.Plugin gravesPlugin = Bukkit.getPluginManager().getPlugin("GravesX");
            if (gravesPlugin == null) {
                return false;
            }

            // Cast plugin to Graves main class and instantiate API
            com.ranull.graves.Graves graves = (com.ranull.graves.Graves) gravesPlugin;
            gravesXAPI = new GravesXAPI(graves);

            enabled = true;
            plugin.getLogger().info("GravesX found and hooked!");
            return true;
        } catch (Throwable e) {
            plugin.getLogger().warning("Failed to hook into GravesX: " + e.getMessage());
            enabled = false;
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean createGrave(Player player, Location location, List<ItemStack> drops, int xp) {
        if (!enabled || gravesXAPI == null) {
            return false;
        }

        try {
            gravesXAPI.createGrave(
                player,
                EntityType.PLAYER,
                location,
                Collections.emptyMap(),
                drops,
                xp,
                System.currentTimeMillis() / 1000
            );
            return true;
        } catch (Throwable t) {
             plugin.getLogger().warning("Failed to create grave via API: " + t.getMessage());
             t.printStackTrace();
             return false;
        }
    }
}
