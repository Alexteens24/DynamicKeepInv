package xyz.superez.dynamickeepinv.hooks;

import com.artillexstudios.axgraves.grave.Grave;
import com.artillexstudios.axgraves.grave.SpawnedGraves;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

import java.util.List;

public class AxGravesHook {
    private final DynamicKeepInvPlugin plugin;
    private boolean enabled;

    public AxGravesHook(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("AxGraves") == null) {
            return false;
        }

        try {
            // Check if class exists to verify dependency
            Class.forName("com.artillexstudios.axgraves.AxGraves");

            enabled = true;
            plugin.getLogger().info("AxGraves found and hooked!");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("AxGraves plugin found but classes missing: " + e.getMessage());
            enabled = false;
            return false;
        } catch (Throwable e) {
            plugin.getLogger().warning("Failed to hook into AxGraves: " + e.getMessage());
            enabled = false;
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean createGrave(Player player, Location location, List<ItemStack> drops, int xp) {
        if (!enabled) {
            return false;
        }

        try {
            // AxGraves creates a grave and adds it to SpawnedGraves
            // Constructor: Grave(Location location, OfflinePlayer player, List<ItemStack> items, int xp, long spawned)
            Grave grave = new Grave(
                location,
                player,
                drops,
                xp,
                System.currentTimeMillis()
            );

            SpawnedGraves.addGrave(grave);
            // Manually trigger update to spawn entity/hologram if needed, though constructor/addGrave likely handles it
            // Based on decompiled structure, 'update()' seems to handle entity spawning/updating
            grave.update();
            return true;
        } catch (Throwable t) {
             plugin.getLogger().warning("Failed to create grave via AxGraves API: " + t.getMessage());
             t.printStackTrace();
             return false;
        }
    }
}
